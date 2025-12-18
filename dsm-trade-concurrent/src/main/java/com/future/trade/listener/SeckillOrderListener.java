package com.future.trade.listener;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.trade.entity.SeckillOrder;
import com.future.trade.entity.enums.OrderStatusEnum;
import com.future.trade.entity.message.SeckillOrderMsg;
import com.future.trade.mapper.SeckillActivityMapper;
import com.future.trade.mapper.SeckillOrderMapper;
import constants.RabbitConstants;
import entity.pojo.info.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rabbitmq.client.Channel;

import java.io.IOException;

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

    @PostConstruct
    public void init() {
        // 强制ERROR级别，确保初始化日志打印
        log.error("【秒杀消费者】初始化完成，监听队列：{}", RabbitConstants.SECKILL_ORDER_QUEUE);
    }

    @RabbitListener(containerFactory = RabbitConstants.SECKILL_RABBIT_LISTENER_CONTAINER_FACTORY,
            queues = RabbitConstants.SECKILL_ORDER_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillOrder(SeckillOrderMsg message,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // 第一步：强制打印消息接收日志（确认消费逻辑触发）
        log.error("【秒杀消费者】接收到消息，订单号：{}，消息体：{}",
                message.getOrderNo(), JSONUtil.toJsonStr(message));

        String orderExistKey = SECKILL_ORDER_EXIST + message.getOrderNo();
        String exists = stringRedisTemplate.opsForValue().get(orderExistKey);

        try {
//            // 1. 幂等性校验
//            if (exists == null) {
//                log.error("【秒杀消费者】订单{}幂等Key不存在，手动ACK", message.getOrderNo());
//                channel.basicAck(deliveryTag, false);
//                return;
//            }

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

            // 4. 创建订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setOrderNo(String.valueOf(message.getOrderNo()));
            seckillOrder.setUserId(message.getUserId());
            seckillOrder.setActivityId(message.getActivityId());
            seckillOrder.setProductId(message.getProductId());
            seckillOrder.setSeckillPrice(Math.toIntExact(message.getPrice()));
            seckillOrder.setOrderStatus(OrderStatusEnum.PENDING_PAY);
            seckillOrder.setProductType(message.getProductType());
            seckillOrderMapper.insert(seckillOrder);

            // 5. 完成：删除Key + ACK
            stringRedisTemplate.delete(orderExistKey);
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
}