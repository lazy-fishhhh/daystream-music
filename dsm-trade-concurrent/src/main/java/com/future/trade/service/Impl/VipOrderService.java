package com.future.trade.service.Impl;

import code.BusinessErrorCode;
import code.UserErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.exception.BusinessException;
import com.future.login.exception.OrderException;
import com.future.login.exception.VipPackageException;
import com.future.login.exception.VipStatusException;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.trade.entity.*;
import com.future.trade.entity.enums.VipPackageEnum;
import com.future.trade.entity.vo.OrderDetailVO;
import com.future.trade.entity.vo.VipStatusVO;
import com.future.trade.mapper.*;
import entity.pojo.info.TaskConfig;
import entity.pojo.info.User;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import message.ExchangeVipMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.future.trade.service.IVipOrderService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import util.RedisTemplateWorker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


import static com.future.trade.constants.VipRedisConstant.*;
import static constants.RabbitConstants.*;
import static constants.RedisCacheConstants.LOCK_TTL;
import static constants.TradeConstants.*;


/**
 *  Vip订单服务
 *  套餐管理
 *  订单处理
 *  支付集成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipOrderService implements IVipOrderService {

    private final VipCoreService vipCoreService;
    private final VipMapper vipMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplateWorker redisTemplateWorker;
    private final VipPackageMapper vipPackageMapper;
    private final UserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;
    private final TradeTaskConfigMapper tradeTaskConfigMapper;

    /*
        注入自身代理对象  —> 解决Spring事务失效原理3：自调用走目标对象而不走代理对象的问题
     */


    // 使用 @Autowired 直接注入自身的代理对象
    // Spring 会自动处理，将包含AOP增强（如事务）的代理对象注入进来
    @Autowired
    // @Lazy 注解告诉 Spring，不要在容器启动时就初始化 selfProxy，
    // 而是在第一次调用 selfProxy 的方法时才进行初始化。
    // 这样，当 VipOrderService 自身初始化完成后，再去初始化 selfProxy，
    // 就不会形成循环依赖。
    @Lazy
    private VipOrderService selfProxy;

    // 另一种方法是从ApplicationContext中获取代理对象

    /**
     * 生成唯一订单号
     * 使用RedisTemplateWorker的全局唯一ID生成器
     */
    private String generateOrderNo() {
        // 使用RedisTemplateWorker生成订单ID，前缀为"vip_order"
        long id = redisTemplateWorker.createId(VIP_ORDER_INFO_PREFIX);
        return String.valueOf(id);
    }



    /**
     * 购买VIP
     */
    @Transactional
    @Override
    public  Result<String> buyVip(String packageName)  {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("尚未登陆");
        }
        Optional<VipPackageEnum> vipPackageEnumOptional = VipPackageEnum.getByPackageName(packageName);
        if (vipPackageEnumOptional.isEmpty()) {
            throw VipPackageException.notFound(packageName);
        }
        VipPackageEnum vipPackageEnum = vipPackageEnumOptional.get();

        // 1. 获取用户当前VIP状态
        VipStatusVO vipStatusVO;
        try {
            vipStatusVO = vipCoreService.checkAndUpdateVipStatus();
        } catch (JsonProcessingException e) {
            throw  VipStatusException.invalidStatus(userId);
        }

        // 2. 检查用户是否已经是VIP了
        // 2.1.1 是VIP，续时长
        if (vipStatusVO.getIsVip() == 1) {
            // 续时长
            Vip vip = vipMapper.selectOne(new LambdaQueryWrapper<Vip>()
                    .eq(Vip::getUserId, userId)
                    .orderByDesc(Vip::getEndTime) // 取最新的一条记录, 避免仅查询id查到过期vip
                    .last("LIMIT 1"));
            if (vip == null) {
                throw VipStatusException.recordNotFound(userId);
            }


            // 2.1.2 更新数据库，删redis
            vip.setEndTime(vip.getEndTime().plusDays(vipPackageEnum.getDurationDays()));
            vipMapper.updateById(vip);
            stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + userId);
            // 2.1.3调用订单服务创建订单
            Order order = selfProxy.createVipOrder(Long.valueOf(vipPackageEnum.getId()), vip);
            // 记录订单ID到VIP记录
            vip.setOrderId(order.getId());
            vipMapper.updateById(vip);
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, userId)
                    // 直接set字段，MyBatis-Plus自动处理LocalDateTime的SQL参数绑定
                    .set(User::getVipExpireTime, vip.getEndTime());
            int affectedRows = userMapper.update(null, updateWrapper);
            if (affectedRows == 0) {
                throw new RuntimeException("更新用户VIP过期时间失败，用户ID：" + userId);
            }
            log.info("更新用户VIP过期时间成功，用户ID：" + userId);

            // 发送兑换会员成功消息
            ExchangeVipMessage message = new ExchangeVipMessage();
            message.setUserId(userId);

            LambdaQueryWrapper<TaskConfig> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TaskConfig::getTaskName, "兑换会员");
            TaskConfig taskConfig = tradeTaskConfigMapper.selectOne(queryWrapper);
            message.setTaskId(taskConfig.getId());
            rabbitTemplate.convertAndSend(
                    EXCHANGE_VIP_SUCCESS_TOPIC_EXCHANGE,
                    ROUTING_KEY_EXCHANGE_VIP_SUCCESS,
                    message
            );

            return Result.ok("VIP续费成功, 有效期至" + vip.getEndTime());
        }

        // 2.2. 不是VIP 插入新记录 并将VipStatus写入redis
        Vip vip = new Vip();
        vip.setUserId(userId);
        vip.setStartTime(LocalDateTime.now());
        vip.setEndTime(LocalDateTime.now().plusDays(vipPackageEnum.getDurationDays()));
        vip.setStatus(Vip.VipStatus.ACTIVE);
        vip.setPackageId(Long.valueOf(vipPackageEnum.getId()));
        vip.setSource(Vip.VipSource.PURCHASE);
        vip.setCreatedTime(LocalDateTime.now());
        vip.setUpdatedTime(LocalDateTime.now());
        vip.setAutoRenew(false);
        vip.setRenewalCount(0);
        vip.setLevel(vipPackageEnum.getLevel());
        vip.setOrderId(null);
        vipMapper.insert(vip);

        VipStatusVO vipStatusVO2 = new VipStatusVO();
        vipStatusVO2.setIsVip(1);
        vipStatusVO2.setRemainingDays(vipPackageEnum.getDurationDays());
        vipStatusVO2.setVipExpireTime(vip.getEndTime());
        vipStatusVO2.setVipLevel(vipPackageEnum.getLevel());
        vipStatusVO2.setCurrentPackageName(String.valueOf(vipPackageEnum.getPackageName()));
        try {
            stringRedisTemplate.opsForValue().set(USER_VIP_INFO_PREFIX + userId,
                    objectMapper.writeValueAsString(vipStatusVO2),
                    VIP_CACHE_USER_INFO_24H,
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 2.2.3 调用订单服务创建订单
        Order order = selfProxy.createVipOrder(Long.valueOf(vipPackageEnum.getId()), vip);
        // 记录订单ID到VIP记录
        vip.setOrderId(order.getId());
        vipMapper.updateById(vip);

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                // 直接set字段，MyBatis-Plus自动处理LocalDateTime的SQL参数绑定
                .set(User::getVipExpireTime, vip.getEndTime());
        int affectedRows = userMapper.update(null, updateWrapper);
        if (affectedRows == 0) {
            throw new RuntimeException("更新用户VIP过期时间失败，用户ID：" + userId);
        }
        log.info("更新用户VIP过期时间成功，用户ID：" + userId);

        // 发送兑换会员成功消息
        ExchangeVipMessage message = new ExchangeVipMessage();
        message.setUserId(userId);
        LambdaQueryWrapper<TaskConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskConfig::getTaskName, "兑换会员");
        TaskConfig taskConfig = tradeTaskConfigMapper.selectOne(queryWrapper);
        message.setTaskId(taskConfig.getId());
        rabbitTemplate.convertAndSend(
                EXCHANGE_VIP_SUCCESS_TOPIC_EXCHANGE,
                ROUTING_KEY_EXCHANGE_VIP_SUCCESS,
                message
        );

        return Result.ok("VIP购买成功, 有效期至" + vip.getEndTime());
    }


    
    /**
     * 创建VIP购买订单
     */
    // 默认就是REQUIRED 内层事务与外层事务合并 ，这里写一下帮助理解
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Order createVipOrder(Long packageId, Vip vip) {
        // 默认VIP只能购买一个套餐这里就不传数量了
        // 1. 验证用户和套餐信息的有效性
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }
        if (packageId == null || packageId <= 0) {
            throw new IllegalArgumentException("无效的套餐ID");
        }

        // 2. 获取VIP套餐信息
        LambdaQueryWrapper<VipPackage> lqw = new LambdaQueryWrapper<>();
                lqw.eq(VipPackage::getId, packageId);
                VipPackage vipPackage = vipPackageMapper.selectOne(lqw);
                if (vipPackage == null) {
                    throw new IllegalArgumentException("套餐不存在");
                }

        // 2. 创建订单对象
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(vipPackage.getCurrentPrice());
        order.setPayAmount(vipPackage.getCurrentPrice()); // 暂不考虑优惠
        order.setOrderType(Order.OrderType.VIP_SUBSCRIBE.getCode());


        // TODO 这里是模拟支付 实际支付会有异步回调 这里先设置为已支付
        // ctrl alt + t 生成try catch 块
//        try {
//             rabbitTemplate.convertAndSend("pay-topic", "pay-success", order.getOrderNo());
//        } catch (Exception e) {
//            log.info("支付成功但是通知发送失败，订单号：{}", order.getOrderNo());
//        }



        order.setStatus(Order.OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());

        // 3. 保存订单
        orderMapper.insert(order);

        // 4. 创建订单详情
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductType(OrderItem.ProductType.VIP_PACKAGE.getCode());
        orderItem.setProductId(packageId);
        orderItem.setProductName(vipPackage.getPackageName());
        orderItem.setPrice(vipPackage.getCurrentPrice());
        orderItem.setQuantity(1);
        orderItem.setEffectiveTime(LocalDateTime.now());
        orderItem.setExpireTime(vip.getEndTime());

        // 4. 保存订单详情
        orderItemMapper.insert(orderItem);

        // 6. 扣减用户余额
        // TODO 这个支付后续看看能不能优化到消息队列
        LambdaQueryWrapper<User> lqw1 = new LambdaQueryWrapper<>();
            lqw1.eq(User::getId, userId);
            User user = userMapper.selectOne(lqw1);
            if (user == null) {
                throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }
            if (user.getWalletBalance() < vipPackage.getCurrentPrice()) {
                throw new BusinessException(BusinessErrorCode.BALANCE_NOT_ENOUGH.getCode(),"余额不足");
            }

            // 5. 扣减用户余额
            user.setWalletBalance(user.getWalletBalance() - vipPackage.getCurrentPrice());
            userMapper.updateById(user);

        // 7. 设置订单详情关联
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(orderItem);
        // TODO 这里是订单详情关联 看后续有没有必要
        order.setOrderItems(orderItems);
        
        // 8. 返回创建的订单
        return order;
    }


    /**
     * 查询用户VIP订单历史
     */
    @Override
    public Result<PageResp<OrderDetailVO>> getUserOrderHistory(PageReq pageReq) {
        try {
            // 1. 校验用户ID
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                log.warn("查询VIP订单历史失败：用户未登录（userId为空）");
                throw new BusinessException(UserErrorCode.LOGIN_STATE_INVALID.getCode(), "用户不存在");
            }
            if (userId <= 0) {
                log.warn("查询VIP订单历史失败：无效的用户ID（userId={}）", userId);
                throw new BusinessException(UserErrorCode.USER_STATUS_ERROR.getCode(), "用户不存在");
            }

            // 2. 查询订单前记录日志（便于追踪）
            log.info("用户[{}]开始查询VIP订单历史", userId);

            // 3. 执行分页查询
            Page<OrderDetailVO> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
            IPage<OrderDetailVO> orders = orderMapper.selectOrderDetailVOList(page, userId);

            // 4. 检查分页结果是否为空
            if (orders.getRecords().isEmpty()) {
                log.info("用户[{}]无VIP订单历史（分页查询结果为空）", userId);
                return Result.ok(new PageResp<>());
            }

            // 5. 构建分页响应对象
            PageResp<OrderDetailVO> pageResp = new PageResp<>();
            pageResp.setRecords(orders.getRecords());
            pageResp.setTotal(orders.getTotal());
            pageResp.setSize(orders.getSize());
            pageResp.setCurrent(orders.getCurrent());
            pageResp.setPages(orders.getPages());

            log.info("用户[{}]查询VIP订单历史成功，共{}条记录", userId, orders.getSize());
            return Result.ok(pageResp);

        } catch (Exception e) {
            // 5. 全局异常捕获（避免接口直接抛出异常）
            log.error("查询用户VIP订单历史时发生异常", e);
            // 6. 抛给全局异常处理器处理
            throw new OrderException(OrderException.OrderError.UNKNOWN_ERROR.getCode(), "查询用户VIP订单历史失败，请稍后重试");
        }
    }

    /**
     * 处理VIP订单退款
     */
    @Transactional
    @Override
    public Result<Void> processOrderRefund(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new OrderException(OrderException.OrderError.ITEM_NOT_FOUND.getCode(), "无效的订单ID");
        }
        
        try {
            // 1. 查询订单信息
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new OrderException(OrderException.OrderError.ITEM_NOT_FOUND.getCode(), "订单不存在");
            }
            
            // 2. 验证订单是否可以退款
            if (order.getStatus() != 1) {
                throw new OrderException(OrderException.OrderError.INVALID_STATUS.getCode(), "只有已支付的订单可以退款");
            }
            
            // 3. 查询订单详情以获取VIP套餐信息
            LambdaQueryWrapper<OrderItem> itemQueryWrapper = new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, orderId)
                    .eq(OrderItem::getProductType, OrderItem.ProductType.VIP_PACKAGE.getCode());
            OrderItem orderItem = orderItemMapper.selectOne(itemQueryWrapper);
            
            if (orderItem == null) {
                throw new OrderException(OrderException.OrderError.ITEM_NOT_FOUND.getCode(), "订单详情不存在");
            }
            
            // 4. 查询用户的VIP信息
            LambdaQueryWrapper<Vip> vipQueryWrapper = new LambdaQueryWrapper<Vip>()
                    .eq(Vip::getUserId, order.getUserId())
                    .eq(Vip::getPackageId, orderItem.getProductId())
                    .orderByDesc(Vip::getCreatedTime);
            Vip vip = vipMapper.selectOne(vipQueryWrapper);
            
            // 5. 计算退款金额（全额退款）
            Integer refundAmount = order.getPayAmount();
            
            // TODO 6. 调用支付服务执行退款（模拟实现）
            boolean refundSuccess = true; // 假设退款成功

            // 7. 更新订单状态为已退款
            order.setStatus(Order.OrderStatus.REFUNDED.getCode());
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);

            // 8. 如果VIP未过期，取消VIP权益
            if (vip != null && vip.isActive()) {
                vip.setStatus(Vip.VipStatus.CANCELLED);
                vip.setUpdatedTime(LocalDateTime.now());
                vipMapper.updateById(vip);

                // 清除用户VIP缓存
                stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + order.getUserId());
            }

            // TODO 9. 记录退款日志和发送通知（模拟实现）
            logRefundInfo(order, refundAmount);

            return Result.ok();
        } catch (Exception e) {
            // 记录错误日志
            e.printStackTrace();
            // 10. 抛给全局异常处理器处理
            throw new OrderException(OrderException.OrderError.REFUND_FAIL.getCode(), "处理退款失败：" + e.getMessage());
        }
    }
    
    /**
     * 记录退款信息
     */
    private void logRefundInfo(Order order, Integer refundAmount) {
        System.out.println(String.format("[%s] 订单退款成功 - 订单号: %s, 用户ID: %d, 退款金额: %d分",
                LocalDateTime.now(), order.getOrderNo(), order.getUserId(), refundAmount));
    }



    /**
     * 定时自动续费任务
     * 每天凌晨2点执行
     */
     @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledAutoRenewal() {
        try {
            // 1. 执行自动续费
            // 自注入代理对象，解决自调用事务失效问题
            selfProxy.processAutoRenewal();

            System.out.println("VIP自动续费定时任务执行完成: " + LocalDateTime.now());
        } catch (Exception e) {
            // 记录定时任务执行异常
            System.err.println("VIP自动续费定时任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 自动续费核心逻辑
     * 筛选用户 -> 扣款 + 下订单 + 更新用户状态 -> 发送通知
     */
//    @Transactional  事务加在这的话粒度太大了吧 所有用户处于同一个事务
    public void processAutoRenewal() {
        //筛选用户 -> 扣款 + 下订单 + 更新用户状态 -> 发送通知


        log.info("开始执行VIP自动续费任务，当前时间：{}", LocalDateTime.now());

        // 1. 筛选待续费用户：开启自动续费 + VIP未过期 + 过期时间在未来3天内
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireThreshold = now.plusDays(3); // 提前3天开始尝试续费
        List<Vip> toRenewVips = vipMapper.selectList(new LambdaQueryWrapper<Vip>()
                .eq(Vip::getAutoRenew, true) // 开启自动续费
                .eq(Vip::getStatus, Vip.VipStatus.ACTIVE) // VIP状态正常
                .between(Vip::getEndTime, now, expireThreshold)); // 即将过期（3天内）

        if (toRenewVips.isEmpty()) {
            log.info("无待续费VIP用户，自动续费任务结束");
            return;
        }

        // 2. 遍历用户执行续费
        for (Vip vip : toRenewVips) {
            Long userId = vip.getUserId();
            Long vipId = vip.getId();
            // 给每个用户单独开启一个事务，确保隔离性
            selfProxy.singleUserRenewal(userId, vipId, now);
        }

        log.info("VIP自动续费任务执行完毕，共处理{}个待续费用户", toRenewVips.size());
    }

    @Transactional //多个 SQL 归为一个事务
    public void singleUserRenewal(Long userId, Long vipId, LocalDateTime now) {
        // 默认续费月套餐
        Long packageId = Long.valueOf(VipPackageEnum.MONTH.getId());
        String lockKey = VIP_RENEW_LOCK + userId; // 分布式锁，防止重复续费

        try {
            // 3. 分布式锁：防止同一用户被多线程重复续费（过期时间30秒）
            Boolean lockSuccess = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, LOCKED_VALUE, LOCK_TTL, java.util.concurrent.TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(lockSuccess)) {
                log.warn("用户[{}]VIP续费失败：获取分布式锁失败（可能正在续费中）", userId);
                return;
            }

            // 4. 再次校验VIP状态（防止并发修改）
            Vip latestVip = vipMapper.selectById(vipId);
            if (latestVip == null || !latestVip.getAutoRenew() || latestVip.getStatus() != Vip.VipStatus.ACTIVE) {
                log.warn("用户[{}]VIP续费失败：自动续费已关闭或VIP状态异常", userId);
                return;
            }

            // 5. 校验套餐状态（是否上架）
            VipPackage vipPackage = vipPackageMapper.selectById(packageId);
            if (vipPackage == null || vipPackage.getStatus() != 1) {
                log.error("用户[{}]VIP续费失败：套餐不存在或已下架，套餐ID：{}", userId, packageId);
                // 关闭自动续费（避免后续重复尝试）
                latestVip.setAutoRenew(false);
                vipMapper.updateById(latestVip);
                // 记录失败日志
//                    recordRenewLog(userId, vipId, null, packageId, vipPackage.getCurrentPrice(), 2, "套餐不存在或已下架");
                return;
            }

            // 6. 校验用户余额
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户[{}]VIP续费失败：用户不存在", userId);
//                    recordRenewLog(userId, vipId, null, packageId, vipPackage.getCurrentPrice(), 2, "用户不存在");
                return;
            }
            if (user.getWalletBalance() < vipPackage.getCurrentPrice()) {
                log.error("用户[{}]VIP续费失败：余额不足（当前余额：{}，所需金额：{}）",
                        userId, user.getWalletBalance(), vipPackage.getCurrentPrice());
                // 关闭自动续费（避免后续重复尝试）
                latestVip.setAutoRenew(false);
                vipMapper.updateById(latestVip);
//                    recordRenewLog(userId, vipId, null, packageId, vipPackage.getCurrentPrice(), 2, "余额不足");
                return;
            }

            // 7. 执行续费：扣减余额 + 延长VIP有效期 + 生成订单
            int renewAmount = vipPackage.getCurrentPrice();

            // 7.1 扣减用户余额
            user.setWalletBalance(user.getWalletBalance() - renewAmount);
            userMapper.updateById(user);

            // 7.2 延长VIP有效期
            LocalDateTime newEndTime = latestVip.getEndTime().plusDays(vipPackage.getDurationDays());
            latestVip.setEndTime(newEndTime);
            latestVip.setRenewalCount(latestVip.getRenewalCount() + 1); // 续费次数+1
            latestVip.setRenewalTime(now); // 更新续费时间
            latestVip.setUpdatedTime(now);
            latestVip.setPackageId(packageId);
            vipMapper.updateById(latestVip);

            // 7.3 生成续费订单（复用createVipOrder方法，注意订单类型标记为“续费”）
            Order renewOrder = selfProxy.createRenewOrder(packageId, userId, vipPackage);
            // 更新VIP关联的订单ID
            latestVip.setOrderId(renewOrder.getId());
            vipMapper.updateById(latestVip);

            // 7.4 清除VIP缓存（刷新用户VIP状态）
            stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + userId);

            // 7.5 记录续费成功日志
            //续费日志
//                recordRenewLog(userId, vipId, renewOrder.getId(), packageId, renewAmount, 1, null);
            log.info("用户[{}]VIP续费成功：套餐ID={}，续费金额={}，新过期时间={}，订单号={}",
                    userId, packageId, renewAmount, newEndTime, renewOrder.getOrderNo());

        } catch (Exception e) {
            log.error("用户[{}]VIP续费异常", userId, e);
            //续费日志
//                recordRenewLog(userId, vipId, null, packageId, 0, 2, e.getMessage());
        } finally {
            // 释放分布式锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 生成续费订单（区别于新购订单，订单类型标记为“续费”）
     */
    private Order createRenewOrder(Long packageId, Long userId, VipPackage vipPackage) {
        Order order = new Order();
        order.setOrderNo(generateRenewOrderNo()); // 续费订单号（前缀区分）
        order.setUserId(userId);
        order.setTotalAmount(vipPackage.getCurrentPrice());
        order.setPayAmount(vipPackage.getCurrentPrice());
        order.setOrderType(Order.OrderType.VIP_SUBSCRIBE.getCode()); // 订单类型：VIP订阅
        order.setStatus(Order.OrderStatus.PAID.getCode()); // 自动续费默认已支付
        order.setPayTime(LocalDateTime.now());
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 创建订单详情
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductType(OrderItem.ProductType.VIP_PACKAGE.getCode());
        orderItem.setProductId(packageId);
        orderItem.setProductName(vipPackage.getPackageName() + "（自动续费）");
        orderItem.setPrice(vipPackage.getCurrentPrice());
        orderItem.setQuantity(1);
        orderItem.setEffectiveTime(LocalDateTime.now());
        orderItem.setExpireTime(LocalDateTime.now().plusDays(vipPackage.getDurationDays()));
        orderItemMapper.insert(orderItem);

        return order;
    }

    /**
     * 生成续费订单号（前缀REFUND_，与新购订单区分）
     */
    private String generateRenewOrderNo() {
        long id = redisTemplateWorker.createId("vip_renew_order");
        return VIP_RENEW_PREFIX + id;
    }

//    /**
//     * 记录续费日志 目前还没有这张表
//     */
//    private void recordRenewLog(Long userId, Long vipId, Long orderId, Long packageId, int amount, int status, String failReason) {
//        VipRenewLog log = new VipRenewLog();
//        log.setUserId(userId);
//        log.setVipId(vipId);
//        log.setOrderId(orderId);
//        log.setPackageId(packageId);
//        log.setRenewAmount(amount);
//        log.setRenewTime(LocalDateTime.now());
//        log.setRenewStatus(status);
//        log.setFailReason(failReason);
//        vipRenewLogMapper.insert(log);
//    }



}
