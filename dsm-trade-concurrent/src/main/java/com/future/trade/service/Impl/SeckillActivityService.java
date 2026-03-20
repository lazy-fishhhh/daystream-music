package com.future.trade.service.Impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.trade.constants.SeckillConstants;
import com.future.trade.constants.SeckillErrorCode;
import com.future.trade.entity.SeckillActivity;
import com.future.trade.entity.VipPackage;
import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.entity.enums.ProductTypeEnum;
import com.future.trade.entity.message.SeckillOrderMsg;
import com.future.trade.entity.vo.SeckillActivityVO;
import com.future.trade.mapper.SeckillActivityMapper;
import com.future.trade.mapper.SeckillOrderMapper;
import com.future.trade.mapper.VipPackageMapper;
import com.future.trade.service.ISeckillActivityService;
import constants.RabbitConstants;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import util.RedisTemplateWorker;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.future.trade.constants.BasicOrderConstants.*;
import static com.future.trade.constants.SeckillConstants.*;
import static constants.RedisCacheConstants.CACHE_NORMAL_TTL;
import static constants.RedisCacheConstants.CACHE_SHORT_TTL;

/**
 * 秒杀活动服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillActivityService implements ISeckillActivityService {
    private final SeckillActivityMapper seckillActivityMapper;
    private final VipPackageMapper vipPackageMapper;
    private final RedisTemplateWorker redisTemplateWorker;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final UserMapper userMapper;

    // lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }



    /**
     * 新增秒杀活动
     * 1. 参数二次校验（防绕过前端校验）
     * 2. 业务规则校验（时间、库存、商品有效性）
     * 3. 事务保证（数据一致性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 异常回滚，保证数据一致性
    public Result<String> addSeckillActivity(@Valid SeckillActivityDTO dto) {
        try {
            // 打印请求日志（便于排查问题）
            log.info("新增秒杀活动请求参数：{}", dto);

            // 1. 基础参数二次校验
            validateBaseParam(dto);

            // 2. 业务规则校验
            validateBusinessRule(dto);

            // 3. 商品有效性校验
            validateProductValid(dto.getProductType(), dto.getProductId());

            // 4. 构建秒杀活动实体
            SeckillActivity activity = buildSeckillActivity(dto);

            // 5. 保存到数据库
            int insertRow = seckillActivityMapper.insert(activity);
            if (insertRow != 1) {
                log.error("新增秒杀活动失败：数据库插入影响行数异常，activity={}", activity);
                throw new BusinessException(SeckillErrorCode.SYSTEM_ERROR.getCode(), "秒杀活动创建失败");
            }
            // 6. 将秒杀活动写入redis,无需有效期可以永久保存
            stringRedisTemplate.opsForValue().set(SECKILL_ACTIVITY_STOCK_PREFIX + activity.getId(), String.valueOf(activity.getTotalStock()));

            log.info("新增秒杀活动成功：activityId={}, activityName={}", activity.getId(), activity.getActivityName());
            return Result.ok("新增秒杀活动成功（ID：" + activity.getId() + "）");

        } catch (BusinessException e) {
            // 捕获业务异常（已知错误，直接返回给前端）
            log.warn("新增秒杀活动业务异常：{}，参数：{}", e.getMessage(), dto);
            throw e;
        } catch (Exception e) {
            // 捕获系统异常（未知错误，隐藏敏感信息）
            log.error("新增秒杀活动系统异常：参数={}", dto, e); // 打印堆栈信息（排查问题）
            throw new BusinessException(SeckillErrorCode.SYSTEM_ERROR.getCode(), SeckillErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 1. 基础参数二次校验（防绕过前端）
     * @param dto 秒杀活动DTO
     * @param activityId 活动ID（修改时传入，用于排除自身）
     */
    private void validateBaseParam(SeckillActivityDTO dto, Long... activityId) {
        // 活动名称非空且去重（避免重复活动）
        if (!StringUtils.hasText(dto.getActivityName())) {
            throw new BusinessException(SeckillErrorCode.PARAM_ERROR.getCode(), "活动名称不能为空");
        }
        // 校验活动名称是否已存在
        LambdaQueryWrapper<SeckillActivity> queryWrapper = new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getActivityName, dto.getActivityName());
        // 如果提供了活动ID，则排除当前活动
        if (activityId != null && activityId.length > 0 && activityId[0] != null) {
            queryWrapper.ne(SeckillActivity::getId, activityId[0]);
        }
        long nameCount = seckillActivityMapper.selectCount(queryWrapper);
        if (nameCount > 0) {
            throw new BusinessException(SeckillErrorCode.ACTIVITY_NAME_DUPLICATE.getCode(), SeckillErrorCode.ACTIVITY_NAME_DUPLICATE.getMessage());
        }

        // 商品名称长度不可超过xxx字
        if (dto.getActivityName().length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new BusinessException(SeckillErrorCode.PRODUCT_NAME_LENGTH_ERROR.getCode(), SeckillErrorCode.PRODUCT_NAME_LENGTH_ERROR.getMessage());
        }

        // 商品ID非空
        if (dto.getProductId() == null || dto.getProductId() <= 0) {
            throw new BusinessException(SeckillErrorCode.PARAM_ERROR.getCode(), "商品ID必须为正整数");
        }

        // 库存范围校验（1-100000，防止超大数据量导致性能问题）
        if (dto.getTotalStock() == null || dto.getTotalStock() < 1 || dto.getTotalStock() > MAX_SECKILL_STOCK) {
            throw new BusinessException(SeckillErrorCode.STOCK_OUT_OF_RANGE.getCode(), SeckillErrorCode.STOCK_OUT_OF_RANGE.getMessage());
        }

        // 价格校验（不能为负数）
        if (dto.getSeckillPrice() == null || dto.getSeckillPrice() < 0) {
            throw new BusinessException(SeckillErrorCode.PRICE_ILLEGAL.getCode(), SeckillErrorCode.PRICE_ILLEGAL.getMessage());
        }

        // 时间非空校验
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException(SeckillErrorCode.PARAM_ERROR.getCode(), "活动开始时间和结束时间不能为空");
        }
    }

    /**
     * 2. 业务规则校验
     */
    private void validateBusinessRule(SeckillActivityDTO dto) {
        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();
        LocalDateTime now = LocalDateTime.now();

        // 时间逻辑校验：开始时间晚于当前时间，结束时间晚于开始时间
        if (startTime.isBefore(now) || startTime.isEqual(now)) {
            throw new BusinessException(SeckillErrorCode.TIME_LOGIC_ERROR.getCode(), "活动开始时间必须晚于当前时间");
        }
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new BusinessException(SeckillErrorCode.TIME_LOGIC_ERROR.getCode(), "活动结束时间必须晚于开始时间");
        }

        // 活动时长限制（≤7天，防止长期活动占用资源）
        long activityDays = Duration.between(startTime, endTime).toDays();
        if (activityDays > MAX_SECKILL_DAY) {
            throw new BusinessException(SeckillErrorCode.TIME_LOGIC_ERROR.getCode(), "活动时长不能超过" + MAX_SECKILL_DAY + "天");
        }
    }

    /**
     * 3. 商品有效性校验
     */
    private void validateProductValid(Integer productType, Long productId) {
        ProductTypeEnum typeEnum = ProductTypeEnum.getByCode(productType);
        if (typeEnum == null) {
            throw new BusinessException(SeckillErrorCode.PRODUCT_TYPE_INVALID.getCode(), SeckillErrorCode.PRODUCT_TYPE_INVALID.getMessage());
        }

        // 根据商品类型校验对应商品是否存在且有效
        switch (typeEnum) {
            case VIP_PACKAGE:
                // 校验会员套餐：存在 + 上架（status=1） + 秒杀专属套餐（package_type=SECKILL）
                VipPackage vipPackage = vipPackageMapper.selectById(productId);
                if (vipPackage == null) {
                    throw new BusinessException(SeckillErrorCode.PRODUCT_NOT_FOUND.getCode(), "会员套餐不存在");
                }
                if (vipPackage.getStatus() != 1) {
                    throw new BusinessException(SeckillErrorCode.PRODUCT_NOT_FOUND.getCode(), "会员套餐已下架");
                }

                break;

            case PHYSICAL_GOODS:
                // TODO 实现实物商品校验逻辑
                break;

            case VIRTUAL_GOODS:
                // TODO 实现虚拟商品校验逻辑（专辑/单曲）
                break;
        }
    }

    /**
     * 4. 构建秒杀活动实体（保证数据一致性）
     */
    private SeckillActivity buildSeckillActivity(SeckillActivityDTO dto) {
        SeckillActivity activity = new SeckillActivity();
        activity.setActivityName(dto.getActivityName());
        activity.setProductType(ProductTypeEnum.getByCode(dto.getProductType()));
        activity.setProductId(dto.getProductId());
        activity.setTotalStock(dto.getTotalStock());
        activity.setRemainingStock(dto.getTotalStock()); // 剩余库存初始值 = 总库存（关键，避免库存为null）
        activity.setSeckillPrice(dto.getSeckillPrice());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setStatus(0); // 初始状态：0-未开始（固定值，避免前端传参错误）
        // createTime、updateTime 由数据库自动填充，无需手动设置
        return activity;
    }



    /**
     * 生成秒杀订单号
     */
    public Long generateSeckillOrderNo() {
        return redisTemplateWorker.createId(SECKILL_ORDER_NO_PREFIX);
    }


    /**
     *  基础秒杀商品校验 + lua购买资格校验、减redis库存
     *      异常处理：简单的异常
     *  然后消息队列  扣减余额、减db库存、生成订单
     *      异常处理：严重的异常
     */
    public Result<String> purchaseSeckillProduct(Long seckillActivityId) {
        // 1. 获取当前用户ID
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("401", "请先登录");
        }

        // 2. 前置校验：活动有效性
        SeckillActivity activity = seckillActivityMapper.selectById(seckillActivityId);
        if (activity == null) {
            throw new BusinessException("404", "秒杀活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime().isAfter(now)) {
            throw new BusinessException("400", "秒杀活动未开始");
        }
        if (activity.getEndTime().isBefore(now)) {
            throw new BusinessException("400", "秒杀活动已结束");
        }

        // 3. Lua脚本原子校验：库存+一人一单
        Long luaResult = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                seckillActivityId.toString(),
                userId.toString()
        );

        // 4. 处理Lua结果
        if (luaResult == null) {
            throw new BusinessException("500", "秒杀失败，请重试");
        }
        int result = luaResult.intValue();
        if (result == 400) {
            throw new BusinessException("400", "已售罄，下次手速要更快哦~");
        }
        if (result == 401) {
            throw new BusinessException("401", "您已参与过秒杀，不可二次参与~");
        }
        if (result == 500) {
            log.info("redis中未查询到秒杀活动,秒杀活动已过期");
            throw new BusinessException("500", "秒杀活动已过期");
        }

        // 5. 生成唯一订单号
        long orderNo = generateSeckillOrderNo();

//        // 6. 设置幂等性Key（30分钟过期）
//        String orderExistKey = SeckillConstants.SECKILL_ORDER_EXIST + orderNo;
//        stringRedisTemplate.opsForValue().set(orderExistKey, "value", 24 * 60 * 60, TimeUnit.SECONDS);

        // 7. 构建消息体
        SeckillOrderMsg msg = new SeckillOrderMsg();
        msg.setActivityId(seckillActivityId);
        msg.setUserId(userId);
        msg.setOrderNo(orderNo);
        msg.setPrice(Long.valueOf(activity.getSeckillPrice()));
        msg.setProductType(activity.getProductType());
        msg.setProductId(activity.getProductId());

        // 8. 记录待处理订单（补偿用）
        String pendingKey = SeckillConstants.SECKILL_PENDING_ORDER + orderNo;
        stringRedisTemplate.opsForValue().set(
                pendingKey,
                JSONUtil.toJsonStr(msg),
                CACHE_NORMAL_TTL,
                TimeUnit.SECONDS
        );

        // 发送MQ消息
        try {
            log.info("开始发送秒杀MQ消息，订单号：{}", orderNo);
            log.info("交换机名称：{}", RabbitConstants.SECKILL_ORDER_EXCHANGE);
            log.info("路由键：{}", RabbitConstants.ROUTING_KEY_SECKILL_ORDER);
            log.info("消息内容：{}", JSONUtil.toJsonStr(msg));
            rabbitTemplate.convertAndSend(
                    RabbitConstants.SECKILL_ORDER_EXCHANGE,
                    RabbitConstants.ROUTING_KEY_SECKILL_ORDER,
                    msg
            );
            log.info("秒杀MQ消息发送请求已提交，订单号：{}", orderNo);
        } catch (Exception e) {
            // 本地异常（如连接超时、序列化失败）
            log.error("发送秒杀消息失败，订单号：{}", orderNo, e);
            // 回滚逻辑...
            throw new BusinessException("500", "秒杀失败，请重试");
        }

        return Result.ok("秒杀成功, 订单号" + orderNo);
    }


    // 优化前的秒杀功能 一人一单Synchronized锁 + 乐观锁扣减库存 + 解决自调用
//    @Override
//    public Result<String> purchaseSeckillProduct(Long seckillActivityId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//
//        // 1. 校验秒杀活动是否存在且已开始
//        SeckillActivity activity = seckillActivityMapper.selectById(seckillActivityId);
//        if (activity == null) {
//            throw new BusinessException(SeckillErrorCode.ACTIVITY_NOT_FOUND.getCode(), "秒杀活动不存在");
//        }
//        if (activity.getStartTime().isAfter(LocalDateTime.now())) {
//            throw new BusinessException(SeckillErrorCode.ACTIVITY_NOT_STARTED.getCode(), "秒杀活动未开始");
//        }
//        if (activity.getEndTime().isBefore(LocalDateTime.now())) {
//            throw new BusinessException(SeckillErrorCode.ACTIVITY_ENDED.getCode(), "秒杀活动已结束");
//        }
//
//        // 注意1：这里使用userId.toString().intern()确保不同用户ID的锁对象不同，避免不同用户之间的锁竞争
//        // 注意2：synchronized 锁不可以在@Transaction的方法内部使用，因为会先释放锁后获取锁，导致锁无法生效
//        // 释放锁，此时事务还未提交，其他线程可以进入锁，引发并发安全问题
//        // 因此，正确执行顺序是：先获取锁，再提交事务。也就是锁住@Transactional
//        // 注意3: 自调用问题
//        synchronized (userId.toString().intern()) {
//            // 解决自调用问题
//            // 获取代理对象，确保在事务中调用
//            ISeckillActivityService proxy = (ISeckillActivityService) AopContext.currentProxy();
//            SeckillOrder seckillOrder = proxy.createSeckillOrder(seckillActivityId, userId, activity);
//            //事务提交后，才会释放锁
//            return Result.ok("秒杀成功，订单号：" + seckillOrder.getOrderNo());
//        }
//
//    }
//
//    @Transactional(rollbackFor = Exception.class) // 所有异常都回滚
//    @NotNull
//    public SeckillOrder createSeckillOrder(Long seckillActivityId, Long userId, SeckillActivity activity) {
//        // 2.一人一单
//        // 查询用户是否已购买过该商品
//            SeckillOrder existingOrder = seckillOrderMapper.selectOne(
//                    new LambdaQueryWrapper<SeckillOrder>()
//                            .eq(SeckillOrder::getUserId, userId)
//                            .eq(SeckillOrder::getActivityId, seckillActivityId)
//                            .eq(SeckillOrder::getOrderStatus, OrderStatusEnum.PENDING_PAY)
//            );
//            if (existingOrder != null) {
//                throw new BusinessException(SeckillErrorCode.ORDER_ALREADY_EXISTS.getCode(), "您已秒杀过该商品");
//            }
//
//
//            // 3. 校验库存是否充足, 库存充足则扣减库存
//            // 乐观锁思想修改库存，避免并发下的库存超售
//            LambdaUpdateWrapper<SeckillActivity> updateWrapper = new LambdaUpdateWrapper<>();
//            // 错误经历：一定不能用上面查询到的的activity.getRemainingStock() - 1
//            // 因为先查再改没有原子性，线程A线程执行到此处时，线程B线程也执行到此处，
//            // 那么线程A线程的先用100 - 1 = 99
//            // 线程B线程也用100 - 1 = 99
//            // 那么线程B线程的更新就会把线程A线程的更新给覆盖掉
//            // 正确方法是用数据库的remaining_stock字段减1
//            updateWrapper.setSql("remaining_stock = remaining_stock - 1")
//                    .eq(SeckillActivity::getId, seckillActivityId)
//                    .gt(SeckillActivity::getRemainingStock, 0);
//
//            int updated = seckillActivityMapper.update(null, updateWrapper);
//            if (updated <= 0) {
//                throw new BusinessException(SeckillErrorCode.ACTIVITY_FULL.getCode(), "秒杀活动已售罄");
//            }
//
//            // 4. 创建秒杀订单
//            SeckillOrder seckillOrder = new SeckillOrder();
//            seckillOrder.setActivityId(seckillActivityId);
//            seckillOrder.setOrderNo(String.valueOf(generateSeckillOrderNo()));
//            seckillOrder.setUserId(userId);
//            seckillOrder.setProductType(activity.getProductType());
//            seckillOrder.setProductId(activity.getProductId());
//            seckillOrder.setSeckillPrice(activity.getSeckillPrice());
//            seckillOrder.setOrderStatus(OrderStatusEnum.PENDING_PAY); // 初始状态：0-待支付（固定值，避免前端传参错误）
//            // createTime、updateTime 由数据库自动填充，无需手动设置
//            seckillOrderMapper.insert(seckillOrder);
//
//            //  MQ同步秒杀订单到普通订单表
//            return seckillOrder;
//        }

    /**
     * 获取所有秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 所有秒杀商品分页列表
     */
    @Override
    public Result<PageResp<SeckillActivityVO>> getAllSeckillProducts(PageReq pageReq) {
        try {
            log.info("开始查询所有秒杀商品，分页参数：pageNum={}, pageSize={}", pageReq.getPageNum(), pageReq.getPageSize());

            // 创建分页对象
            Page<SeckillActivity> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
            
            // 执行连表分页查询
            IPage<SeckillActivityVO> activityVOPage = seckillActivityMapper.selectAllSeckillActivities(page, null);
            
            // 构建分页响应
            PageResp<SeckillActivityVO> pageResp = new PageResp<>();
            pageResp.setRecords(activityVOPage.getRecords());
            pageResp.setTotal(activityVOPage.getTotal());
            pageResp.setSize(activityVOPage.getSize());
            pageResp.setCurrent(activityVOPage.getCurrent());
            pageResp.setPages(activityVOPage.getPages());
            
            log.info("查询所有秒杀商品完成，共{}条记录", activityVOPage.getTotal());
            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("查询所有秒杀商品失败：", e);
            return Result.fail(SeckillErrorCode.ACTIVITY_NOT_FOUND.getMessage());
        }
    }

    /**
     * 获取即将开启的秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 即将开启的秒杀商品分页列表
     */
    @Override
    public Result<PageResp<SeckillActivityVO>> getUpcomingSeckillProducts(PageReq pageReq) {
        try {
            log.info("开始查询即将开启的秒杀商品，分页参数：pageNum={}, pageSize={}", pageReq.getPageNum(), pageReq.getPageSize());

            // 创建分页对象
            Page<SeckillActivity> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageReq.getPageNum(), pageReq.getPageSize());
            
            // 执行连表分页查询
            IPage<SeckillActivityVO> activityVOPage = seckillActivityMapper.selectUpcomingSeckillActivities(page, LocalDateTime.now(),null);
            
            // 构建分页响应
            PageResp<SeckillActivityVO> pageResp = new PageResp<>();
            pageResp.setRecords(activityVOPage.getRecords());
            pageResp.setTotal(activityVOPage.getTotal());
            pageResp.setSize(activityVOPage.getSize());
            pageResp.setCurrent(activityVOPage.getCurrent());
            pageResp.setPages(activityVOPage.getPages());
            
            log.info("查询即将开启的秒杀商品完成，共{}条记录", activityVOPage.getTotal());
            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("查询即将开启的秒杀商品失败：", e);
            return Result.fail(SeckillErrorCode.ACTIVITY_NOT_FOUND.getMessage());
        }
    }

    /**
     * 获取正在进行中的秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 正在进行中的秒杀商品分页列表
     */
    @Override
    public Result<PageResp<SeckillActivityVO>> getOngoingSeckillProducts(PageReq pageReq) {
        try {
            log.info("开始查询正在进行中的秒杀商品，分页参数：pageNum={}, pageSize={}", pageReq.getPageNum(), pageReq.getPageSize());

            // 创建分页对象
            Page<SeckillActivity> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageReq.getPageNum(), pageReq.getPageSize());
            
            // 执行连表分页查询
            IPage<SeckillActivityVO> activityVOPage = seckillActivityMapper.selectOngoingSeckillActivities(page, LocalDateTime.now(),null);
            
            // 构建分页响应
            PageResp<SeckillActivityVO> pageResp = new PageResp<>();
            pageResp.setRecords(activityVOPage.getRecords());
            pageResp.setTotal(activityVOPage.getTotal());
            pageResp.setSize(activityVOPage.getSize());
            pageResp.setCurrent(activityVOPage.getCurrent());
            pageResp.setPages(activityVOPage.getPages());
            
            log.info("查询正在进行中的秒杀商品完成，共{}条记录", activityVOPage.getTotal());
            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("查询正在进行中的秒杀商品失败：", e);
            return Result.fail(SeckillErrorCode.ACTIVITY_NOT_FOUND.getMessage());
        }
    }

    /**
     * 修改未开始的秒杀商品
     * 1. 参数二次校验（防绕过前端校验）
     * 2. 业务规则校验（时间、库存、商品有效性）
     * 3. 事务保证（数据一致性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateSeckillActivity(Long activityId, @Valid SeckillActivityDTO dto) {
        try {
            log.info("修改秒杀活动请求参数：activityId={}, dto={}", activityId, dto);

            // 1. 检查活动是否存在
            SeckillActivity existingActivity = seckillActivityMapper.selectById(activityId);
            if (existingActivity == null) {
                log.warn("修改秒杀活动失败：活动不存在，activityId={}", activityId);
                throw new BusinessException(SeckillErrorCode.ACTIVITY_NOT_FOUND.getCode(), "秒杀活动不存在");
            }

            if (!existingActivity.getProductType().equals(ProductTypeEnum.getByCode(dto.getProductType())) || !existingActivity.getProductId().equals(dto.getProductId())){
                return Result.fail("不允许修改秒杀商品的类型与id");
            }



            // 2. 检查活动是否已经开始（只有未开始的活动才能修改）
            if (existingActivity.getStartTime().isBefore(LocalDateTime.now()) || existingActivity.getStartTime().isEqual(LocalDateTime.now())) {
                log.warn("修改秒杀活动失败：活动已开始，无法修改，activityId={}", activityId);
                throw new BusinessException(SeckillErrorCode.ACTIVITY_ALREADY_STARTED.getCode(), "活动已开始，无法修改");
            }

            // 3. 基础参数二次校验
            validateBaseParam(dto, activityId);

            // 4. 业务规则校验
            validateBusinessRule(dto);

            // 5. 商品有效性校验
            validateProductValid(dto.getProductType(), dto.getProductId());

            // 6. 更新活动信息
            updateSeckillActivityInfo(existingActivity, dto);

            // 7. 更新到数据库
            int updateRow = seckillActivityMapper.updateById(existingActivity);
            if (updateRow != 1) {
                log.error("修改秒杀活动失败：数据库更新影响行数异常，activityId={}", activityId);
                throw new BusinessException(SeckillErrorCode.SYSTEM_ERROR.getCode(), "秒杀活动修改失败");
            }

            // 8. 更新Redis中的库存信息
            stringRedisTemplate.opsForValue().set(SECKILL_ACTIVITY_STOCK_PREFIX + activityId, String.valueOf(existingActivity.getTotalStock()));

            log.info("修改秒杀活动成功：activityId={}, activityName={}", activityId, existingActivity.getActivityName());
            return Result.ok("修改秒杀活动成功");

        } catch (BusinessException e) {
            log.warn("修改秒杀活动业务异常：{}，参数：activityId={}, dto={}", e.getMessage(), activityId, dto);
            throw e;
        } catch (Exception e) {
            log.error("修改秒杀活动系统异常：activityId={}, dto={}", activityId, dto, e);
            throw new BusinessException(SeckillErrorCode.SYSTEM_ERROR.getCode(), SeckillErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 删除秒杀商品
     * 1. 检查活动是否存在
     * 2. 检查活动是否已经开始（只有未开始的活动才能删除）
     * 3. 事务保证（数据一致性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteSeckillActivity(Long activityId) {
        try {
            log.info("删除秒杀活动请求：activityId={}", activityId);

            // 1. 检查活动是否存在
            SeckillActivity existingActivity = seckillActivityMapper.selectById(activityId);
            if (existingActivity == null) {
                log.warn("删除秒杀活动失败：活动不存在，activityId={}", activityId);
                throw new BusinessException(SeckillErrorCode.ACTIVITY_NOT_FOUND.getCode(), "秒杀活动不存在");
            }

            // 2. 检查活动是否已经开始（只有未开始的活动才能删除）
            if (existingActivity.getStartTime().isBefore(LocalDateTime.now()) || existingActivity.getStartTime().isEqual(LocalDateTime.now())) {
                log.warn("删除秒杀活动失败：活动已开始，无法删除，activityId={}", activityId);
                throw new BusinessException(SeckillErrorCode.ACTIVITY_ALREADY_STARTED.getCode(), "活动已开始，无法删除");
            }

            // 3. 从数据库删除活动
            int deleteRow = seckillActivityMapper.deleteById(activityId);
            if (deleteRow != 1) {
                log.error("删除秒杀活动失败：数据库删除影响行数异常，activityId={}", activityId);
                throw new BusinessException(SeckillErrorCode.SYSTEM_ERROR.getCode(), "秒杀活动删除失败");
            }

            // 4. 从Redis删除活动库存信息
            stringRedisTemplate.delete(SECKILL_ACTIVITY_STOCK_PREFIX + activityId);

            log.info("删除秒杀活动成功：activityId={}, activityName={}", activityId, existingActivity.getActivityName());
            return Result.ok("删除秒杀活动成功");

        } catch (BusinessException e) {
            log.warn("删除秒杀活动业务异常：{}，activityId={}", e.getMessage(), activityId);
            throw e;
        } catch (Exception e) {
            log.error("删除秒杀活动系统异常：activityId={}", activityId, e);
            throw new BusinessException(SeckillErrorCode.SYSTEM_ERROR.getCode(), SeckillErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 更新秒杀活动信息
     */
    private void updateSeckillActivityInfo(SeckillActivity existingActivity, SeckillActivityDTO dto) {
        existingActivity.setActivityName(dto.getActivityName());
        existingActivity.setProductType(ProductTypeEnum.getByCode(dto.getProductType()));
        existingActivity.setProductId(dto.getProductId());
        existingActivity.setTotalStock(dto.getTotalStock());
        existingActivity.setRemainingStock(dto.getTotalStock()); // 重新设置剩余库存为总库存
        existingActivity.setSeckillPrice(dto.getSeckillPrice());
        existingActivity.setStartTime(dto.getStartTime());
        existingActivity.setEndTime(dto.getEndTime());
        // updateTime 由数据库自动填充，无需手动设置
    }


    @Override
    public Result<Boolean> delete(Long activityId) {

        int i = seckillActivityMapper.deleteById(activityId);
        return Result.ok(i > 0);
    }
}
