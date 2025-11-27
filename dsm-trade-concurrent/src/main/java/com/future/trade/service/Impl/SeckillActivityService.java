package com.future.trade.service.Impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.future.login.exception.BusinessException;

import com.future.login.util.SecurityUtil;
import com.future.trade.constants.SeckillErrorCode;
import com.future.trade.entity.SeckillActivity;
import com.future.trade.entity.SeckillOrder;
import com.future.trade.entity.VipPackage;
import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.entity.enums.OrderStatusEnum;
import com.future.trade.entity.enums.ProductTypeEnum;

import com.future.trade.mapper.SeckillActivityMapper;
import com.future.trade.mapper.SeckillOrderMapper;
import com.future.trade.mapper.VipPackageMapper;
import com.future.trade.service.ISeckillActivityService;
import entity.pojo.util.Result;
import jakarta.validation.Valid;
import kotlin.jvm.internal.Lambda;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Synchronize;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import util.RedisTemplateWorker;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.future.trade.constants.BasicOrderConstants.MAX_SECKILL_DAY;
import static com.future.trade.constants.BasicOrderConstants.MAX_SECKILL_STOCK;
import static com.future.trade.constants.SeckillConstants.SECKILL_ORDER_NO_PREFIX;

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
     */
    private void validateBaseParam(SeckillActivityDTO dto) {
        // 活动名称非空且去重（避免重复活动）
        if (!StringUtils.hasText(dto.getActivityName())) {
            throw new BusinessException(SeckillErrorCode.PARAM_ERROR.getCode(), "活动名称不能为空");
        }
        // 校验活动名称是否已存在
        long nameCount = seckillActivityMapper.selectCount(
                new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getActivityName, dto.getActivityName())
        );
        if (nameCount > 0) {
            throw new BusinessException(SeckillErrorCode.ACTIVITY_NAME_DUPLICATE.getCode(), SeckillErrorCode.ACTIVITY_NAME_DUPLICATE.getMessage());
        }


        // 商品ID非空
        if (dto.getProductId() == null || dto.getProductId() <= 0) {
            throw new BusinessException(SeckillErrorCode.PARAM_ERROR.getCode(), "商品ID必须为正整数");
        }

        // 库存范围校验（1-10000，防止超大数据量导致性能问题）
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

    @Override
    public Result<String> purchaseSeckillProduct(Long seckillActivityId) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 1. 校验秒杀活动是否存在且已开始
        SeckillActivity activity = seckillActivityMapper.selectById(seckillActivityId);
        if (activity == null) {
            throw new BusinessException(SeckillErrorCode.ACTIVITY_NOT_FOUND.getCode(), "秒杀活动不存在");
        }
        if (activity.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(SeckillErrorCode.ACTIVITY_NOT_STARTED.getCode(), "秒杀活动未开始");
        }
        if (activity.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(SeckillErrorCode.ACTIVITY_ENDED.getCode(), "秒杀活动已结束");
        }

        // 注意1：这里使用userId.toString().intern()确保不同用户ID的锁对象不同，避免不同用户之间的锁竞争
        // 注意2：synchronized 锁不可以在@Transaction的方法内部使用，因为会先释放锁后获取锁，导致锁无法生效
        // 释放锁，此时事务还未提交，其他线程可以进入锁，引发并发安全问题
        // 因此，正确执行顺序是：先获取锁，再提交事务。也就是锁住@Transactional
        // 注意3: 自调用问题
        synchronized (userId.toString().intern()) {
            // 解决自调用问题
            // 获取代理对象，确保在事务中调用
            ISeckillActivityService proxy = (ISeckillActivityService) AopContext.currentProxy();
            SeckillOrder seckillOrder = proxy.createSeckillOrder(seckillActivityId, userId, activity);
            //事务提交后，才会释放锁
            return Result.ok("秒杀成功，订单号：" + seckillOrder.getOrderNo());
        }

    }

    @Transactional(rollbackFor = Exception.class) // 所有异常都回滚
    @NotNull
    public SeckillOrder createSeckillOrder(Long seckillActivityId, Long userId, SeckillActivity activity) {
        // 2.一人一单
        // 查询用户是否已购买过该商品
            SeckillOrder existingOrder = seckillOrderMapper.selectOne(
                    new LambdaQueryWrapper<SeckillOrder>()
                            .eq(SeckillOrder::getUserId, userId)
                            .eq(SeckillOrder::getActivityId, seckillActivityId)
                            .eq(SeckillOrder::getOrderStatus, OrderStatusEnum.PENDING_PAY)
            );
            if (existingOrder != null) {
                throw new BusinessException(SeckillErrorCode.ORDER_ALREADY_EXISTS.getCode(), "您已秒杀过该商品");
            }


            // 3. 校验库存是否充足, 库存充足则扣减库存
            // 乐观锁思想修改库存，避免并发下的库存超售
            LambdaUpdateWrapper<SeckillActivity> updateWrapper = new LambdaUpdateWrapper<>();
            // 错误经历：一定不能用上面查询到的的activity.getRemainingStock() - 1
            // 因为先查再改没有原子性，线程A线程执行到此处时，线程B线程也执行到此处，
            // 那么线程A线程的先用100 - 1 = 99
            // 线程B线程也用100 - 1 = 99
            // 那么线程B线程的更新就会把线程A线程的更新给覆盖掉
            // 正确方法是用数据库的remaining_stock字段减1
            updateWrapper.setSql("remaining_stock = remaining_stock - 1")
                    .eq(SeckillActivity::getId, seckillActivityId)
                    .gt(SeckillActivity::getRemainingStock, 0);

            int updated = seckillActivityMapper.update(null, updateWrapper);
            if (updated <= 0) {
                throw new BusinessException(SeckillErrorCode.ACTIVITY_FULL.getCode(), "秒杀活动已售罄");
            }

            // 4. 创建秒杀订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setActivityId(seckillActivityId);
            seckillOrder.setOrderNo(String.valueOf(generateSeckillOrderNo()));
            seckillOrder.setUserId(userId);
            seckillOrder.setProductType(activity.getProductType());
            seckillOrder.setProductId(activity.getProductId());
            seckillOrder.setSeckillPrice(activity.getSeckillPrice());
            seckillOrder.setOrderStatus(OrderStatusEnum.PENDING_PAY); // 初始状态：0-待支付（固定值，避免前端传参错误）
            // createTime、updateTime 由数据库自动填充，无需手动设置
            seckillOrderMapper.insert(seckillOrder);

            // TODO MQ同步秒杀订单到普通订单表
            return seckillOrder;
        }


}