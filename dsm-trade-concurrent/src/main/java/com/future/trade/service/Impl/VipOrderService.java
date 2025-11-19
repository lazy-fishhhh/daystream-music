package com.future.trade.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.trade.entity.*;
import com.future.trade.entity.enums.VipPackageEnum;
import com.future.trade.entity.vo.VipStatusVO;
import com.future.trade.mapper.OrderItemMapper;
import com.future.trade.mapper.OrderMapper;
import com.future.trade.mapper.VipMapper;
import com.future.trade.mapper.VipPackageMapper;
import entity.pojo.info.User;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.future.trade.service.IVipOrderService;
import util.RedisTemplateWorker;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.future.trade.constants.VipRedisConstant.*;


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
    public  Result<String> buyVip(String packageName) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("尚未登陆");
        }
        Optional<VipPackageEnum> vipPackageEnumOptional = VipPackageEnum.getByPackageName(packageName);
        if (vipPackageEnumOptional.isEmpty()) {
            throw new RuntimeException("VIP套餐不存在");
        }
        VipPackageEnum vipPackageEnum = vipPackageEnumOptional.get();


        // 1. 获取用户当前VIP状态
        VipStatusVO vipStatusVO;
        try {
            vipStatusVO = vipCoreService.checkAndUpdateVipStatus();// 自调用？
        } catch (JsonProcessingException e) {
            throw new RuntimeException("获取用户VIP状态失败");
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
                throw new RuntimeException("用户VIP记录不存在");
            }


            // 2.1.2 更新数据库，删redis
            vip.setEndTime(vip.getEndTime().plusDays(vipPackageEnum.getDurationDays()));
            vipMapper.updateById(vip);
            stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + userId);

            // 2.1.3调用订单服务创建订单
            Order order = createVipOrder(Long.valueOf(vipPackageEnum.getId()));
            // 记录订单ID到VIP记录
            vip.setOrderId(order.getId());
            vipMapper.updateById(vip);

            return Result.ok("VIP续期成功");
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
        Order order = createVipOrder(Long.valueOf(vipPackageEnum.getId()));
        // 记录订单ID到VIP记录
        vip.setOrderId(order.getId());
        vipMapper.updateById(vip);

        return Result.ok("VIP购买成功");
    }


    
    /**
     * 创建VIP购买订单
     */
    @Transactional  // TODO 这里也有自调用啊 先沉淀一波再解决
    public Order createVipOrder(Long packageId) {
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
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT.getCode());
        // TODO 这里是模拟支付 实际支付会有异步回调 这里先设置为已支付
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
        orderItem.setExpireTime(LocalDateTime.now().plusDays(vipPackage.getDurationDays()));
        
        // 5. 保存订单详情
        orderItemMapper.insert(orderItem);

        // 6. 扣减用户余额
        // TODO 这个支付后续看看能不能优化到消息队列
        LambdaQueryWrapper<User> lqw1 = new LambdaQueryWrapper<>();
            lqw1.eq(User::getId, userId);
            User user = userMapper.selectOne(lqw1);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            if (user.getWalletBalance() < vipPackage.getCurrentPrice()) {
                throw new IllegalArgumentException("余额不足");
            }
            // 扣减用户余额
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
                return Result.fail("用户未登录，请先登录");
            }
            if (userId <= 0) {
                log.warn("查询VIP订单历史失败：无效的用户ID（userId={}）", userId);
                return Result.fail("无效的用户ID，请重试");
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
            throw new RuntimeException("查询用户VIP订单历史失败，请稍后重试");
        }
    }

    /**
     * 处理VIP订单退款
     */
    @Transactional
    @Override
    public Result<Void> processOrderRefund(Long orderId) {
        if (orderId == null || orderId <= 0) {
            return Result.fail("无效的订单ID");
        }
        
        try {
            // 1. 查询订单信息
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.fail("订单不存在");
            }
            
            // 2. 验证订单是否可以退款
            if (order.getStatus() != 1) {
                return Result.fail("只有已支付的订单可以退款");
            }
            
            // 3. 查询订单详情以获取VIP套餐信息
            LambdaQueryWrapper<OrderItem> itemQueryWrapper = new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, orderId)
                    .eq(OrderItem::getProductType, OrderItem.ProductType.VIP_PACKAGE.getCode());
            OrderItem orderItem = orderItemMapper.selectOne(itemQueryWrapper);
            
            if (orderItem == null) {
                return Result.fail("订单详情不存在");
            }
            
            // 4. 查询用户的VIP信息
            LambdaQueryWrapper<Vip> vipQueryWrapper = new LambdaQueryWrapper<Vip>()
                    .eq(Vip::getUserId, order.getUserId())
                    .eq(Vip::getPackageId, orderItem.getProductId())
                    .orderByDesc(Vip::getCreatedTime);
            Vip vip = vipMapper.selectOne(vipQueryWrapper);
            
            // 5. 计算退款金额（简化版：全额退款，实际应按使用比例计算）
            Integer refundAmount = order.getPayAmount();
            
            // 6. 调用支付服务执行退款（模拟实现）
            boolean refundSuccess = true; // 假设退款成功
            
            if (refundSuccess) {
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
                
                // 9. 记录退款日志和发送通知（模拟实现）
                logRefundInfo(order, refundAmount);
                
                return Result.ok();
            } else {
                return Result.fail("退款失败，请重试");
            }
        } catch (Exception e) {
            // 记录错误日志
            e.printStackTrace();
            return Result.fail("处理退款失败：" + e.getMessage());
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
    // @Scheduled(cron = "0 0 2 * * ?") 
    public void scheduledAutoRenewal() {
        try {
            // 1. 调用VipCoreService的processAutoRenewal方法处理自动续费
            vipCoreService.processAutoRenewal();
            
            System.out.println("VIP自动续费定时任务执行完成: " + LocalDateTime.now());
        } catch (Exception e) {
            // 记录定时任务执行异常
            System.err.println("VIP自动续费定时任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
