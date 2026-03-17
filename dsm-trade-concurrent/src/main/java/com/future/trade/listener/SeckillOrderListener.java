package com.future.trade.listener;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.trade.entity.Order;
import com.future.trade.entity.OrderItem;
import com.future.trade.entity.SeckillOrder;
import com.future.trade.entity.VipPackage;
import com.future.trade.entity.enums.OrderStatusEnum;
import com.future.trade.entity.enums.ProductTypeEnum;
import com.future.trade.entity.message.SeckillOrderMsg;
import com.future.trade.mapper.*;
import constants.RabbitConstants;
import entity.pojo.info.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.time.LocalDateTime;

import static code.BusinessErrorCode.BALANCE_NOT_ENOUGH;
import static com.future.trade.constants.SeckillConstants.SECKILL_ORDER_EXIST;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderListener {
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final OrderMapper orderMapper;
    private final VipPackageMapper vipPackageMapper;
    private final OrderItemMapper orderItemMapper;

    @Autowired
    @Lazy
    private  SeckillOrderListener proxy;

    @PostConstruct
    public void init() {
        // 强制ERROR级别，确保初始化日志打印
        log.info("【秒杀消费者】初始化完成，监听队列：{}", RabbitConstants.SECKILL_ORDER_QUEUE);
    }

    @RabbitListener(containerFactory = RabbitConstants.SECKILL_RABBIT_LISTENER_CONTAINER_FACTORY,
            queues = RabbitConstants.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(SeckillOrderMsg message,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // 第一步：强制打印消息接收日志（确认消费逻辑触发）
        log.info("【秒杀消费者】接收到消息，订单号：{}，消息体：{}",
                message.getOrderNo(), JSONUtil.toJsonStr(message));

        String orderExistKey = SECKILL_ORDER_EXIST + message.getOrderNo();
//        String exists = stringRedisTemplate.opsForValue().get(orderExistKey);

        try {
//            // 1. 幂等性校验
//            if (exists == null) {
//                log.error("【秒杀消费者】订单{}幂等Key不存在，手动ACK", message.getOrderNo());
//                channel.basicAck(deliveryTag, false);
//                return;
//            }
            // ========== 数据库幂等校验 ==========
            int seckillOrderCount = Math.toIntExact(seckillOrderMapper.selectCount(
                    new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getOrderNo, message.getOrderNo())
            ));
            
            if (seckillOrderCount > 0) {
                // 检查OrderItem是否已存在
                int orderItemCount = Math.toIntExact(orderItemMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, message.getOrderNo())
                ));
                
                if (orderItemCount > 0) {
                    log.warn("【幂等拦截】订单{}及详情已存在，跳过重复处理", message.getOrderNo());
                    channel.basicAck(deliveryTag, false);
                    return;
                } else {
                    log.warn("【幂等处理】订单{}已存在但订单详情不存在，继续处理", message.getOrderNo());
                    // 不返回，继续执行后续逻辑
                }
            }


            // 订单核心业务
            proxy.coreOrderHandler(message);

            // 5. 完成：删除Key + ACK
//            stringRedisTemplate.delete(orderExistKey);
            channel.basicAck(deliveryTag, false);
            log.error("【秒杀消费者】订单{}处理成功", message.getOrderNo());

        } catch (Exception e) {
            // 异常：回滚 + NACK（触发死信）
            log.error("【秒杀消费者】订单{}处理失败", message.getOrderNo(), e);
            stringRedisTemplate.opsForValue().increment("SECKILL:ACTIVITY:STOCK:" + message.getActivityId(), 1);
            seckillActivityMapper.increaseStock(message.getActivityId());
            stringRedisTemplate.delete(orderExistKey);
            channel.basicNack(deliveryTag, false, false);
            throw e;
        }
    }


    @Transactional
    public void coreOrderHandler(SeckillOrderMsg message) {

        // 检查SeckillOrder是否已存在
        SeckillOrder existingSeckillOrder = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getOrderNo, message.getOrderNo())
        );
        
        Order order = null;
        
        if (existingSeckillOrder == null) {
            // 第一次处理：完整流程
            log.info("【订单处理】第一次处理订单{}", message.getOrderNo());
            
            // 2. 扣减用户余额
            LambdaUpdateWrapper<User> balanceWrapper = new LambdaUpdateWrapper<>();
            balanceWrapper.eq(User::getId, message.getUserId())
                    .ge(User::getWalletBalance, message.getPrice())
                    .setSql("wallet_balance = wallet_balance - " + message.getPrice());
            int balanceUpdateCount = userMapper.update(null, balanceWrapper);
            if (balanceUpdateCount == 0) {
                throw new BusinessException(BALANCE_NOT_ENOUGH.getCode(), "用户余额不足");
            }

            // 3. 扣减库存
            int stockUpdateCount = seckillActivityMapper.decreaseStock(message.getActivityId());
            if (stockUpdateCount == 0) {
                throw new BusinessException("400", "数据库库存不足");
            }

            // 4.1 创建订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setOrderNo(String.valueOf(message.getOrderNo()));
            seckillOrder.setUserId(message.getUserId());
            seckillOrder.setActivityId(message.getActivityId());
            seckillOrder.setProductId(message.getProductId());
            seckillOrder.setSeckillPrice(Math.toIntExact(message.getPrice()));
            seckillOrder.setOrderStatus(OrderStatusEnum.COMPLETED);
            seckillOrder.setProductType(message.getProductType());
            seckillOrderMapper.insert(seckillOrder);

            // 4.2 秒杀订单写入普通订单
            order = new Order();
            order.setOrderNo(String.valueOf(message.getOrderNo()));
            order.setUserId(message.getUserId());
            order.setTotalAmount(Math.toIntExact(message.getPrice()));
            order.setPayAmount(Math.toIntExact(message.getPrice()));
            order.setOrderType(1);
            order.setStatus(OrderStatusEnum.COMPLETED.getCode());
            orderMapper.insert(order);
            log.info("777777创建订单成功，订单号：{}", message.getOrderNo());
        } else {
            // 补单处理：只创建OrderItem
            log.info("【订单处理】补单处理订单{}", message.getOrderNo());
            
            // 查询已存在的Order
            order = orderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, String.valueOf(message.getOrderNo()))
            );
            
            if (order == null) {
                log.error("【订单处理】订单{}的Order不存在，无法补单", message.getOrderNo());
                throw new BusinessException("400", "订单不存在");
            }
        }

        // 4.3 order_item 订单详情同步
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductType(message.getProductType().getCode());
        orderItem.setProductId(message.getProductId());

        if (message.getProductType() == ProductTypeEnum.VIP_PACKAGE) {
            VipPackage vipPackage = vipPackageMapper.selectById(message.getProductId());
            if (vipPackage != null) {
                orderItem.setProductName(vipPackage.getPackageName());
                orderItem.setEffectiveTime(LocalDateTime.now());
                orderItem.setExpireTime(LocalDateTime.now().plusDays(vipPackage.getDurationDays()));
            } else {
                orderItem.setProductName("VIP套餐");
            }
        }
        // TODO 暂不支持VIP以外的商品类型
        else if (message.getProductType() == ProductTypeEnum.PHYSICAL_GOODS) {
            orderItem.setProductName("实物商品");
        }else if (message.getProductType() == ProductTypeEnum.VIRTUAL_GOODS) {
            orderItem.setProductName("虚拟商品");
        }else {
            orderItem.setProductName("其他商品");
        }

        orderItem.setPrice(Math.toIntExact(message.getPrice()));
        orderItem.setQuantity(1);

        // 打印orderItem信息
        log.info("【订单详情】准备插入orderItem: orderId={}, productType={}, productId={}, productName={}, price={}", 
                orderItem.getOrderId(), orderItem.getProductType(), orderItem.getProductId(), 
                orderItem.getProductName(), orderItem.getPrice());

        // 4. 保存订单详情
        int insertResult = orderItemMapper.insert(orderItem);
        log.info("777777777保存订单详情成功，订单号：{}，插入结果：{}", message.getOrderNo(), insertResult);
    }
}
