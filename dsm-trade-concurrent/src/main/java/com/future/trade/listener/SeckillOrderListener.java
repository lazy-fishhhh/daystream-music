package com.future.trade.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.trade.constants.RabbitConstants;
import com.future.trade.entity.SeckillOrder;
import com.future.trade.entity.enums.OrderStatusEnum;
import com.future.trade.entity.message.SeckillOrderMsg;
import com.future.trade.mapper.SeckillActivityMapper;
import com.future.trade.mapper.SeckillOrderMapper;
import entity.pojo.info.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    /**
     * 监听秒杀订单队列（幂等性、原子扣余额、库存一致性）
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitConstants.SECKILL_ORDER_QUEUE, durable = "true", declare = "true"),
            exchange = @Exchange(value = RabbitConstants.SECKILL_ORDER_EXCHANGE,
                                 type = ExchangeTypes.TOPIC, durable = "true"),
            key = RabbitConstants.ROUTING_KEY_SECKILL_ORDER
    ))
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillOrder(SeckillOrderMsg message) {
        String orderExistKey = SECKILL_ORDER_EXIST + message.getOrderNo();
        // 1. 幂等性校验
        String exists = stringRedisTemplate.opsForValue().get(orderExistKey);
        if (exists == null) {
            log.info("订单{}已处理或重复消费，跳过", message.getOrderNo());
            return;
        }

        try {
            // 2. 原子扣减用户余额（余额校验，防止扣负）
            LambdaUpdateWrapper<User> balanceWrapper = new LambdaUpdateWrapper<>();
            balanceWrapper.eq(User::getId, message.getUserId())
                    .ge(User::getWalletBalance, message.getPrice()) // 原子校验：余额>=秒杀价
                    .setSql("wallet_balance = wallet_balance - " + message.getPrice());
            int balanceUpdateCount = userMapper.update(null, balanceWrapper);
            if (balanceUpdateCount == 0) {
                throw new BusinessException(BALANCE_NOT_ENOUGH.getCode(), "用户余额不足");
            }

            // 3. 扣减数据库库存（乐观锁防超卖）
            int stockUpdateCount = seckillActivityMapper.decreaseStock(message.getActivityId());
            if (stockUpdateCount == 0) {
                throw new BusinessException("400", "数据库库存不足");
            }

            // 4. 创建秒杀订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setOrderNo(String.valueOf(message.getOrderNo()));
            seckillOrder.setUserId(message.getUserId());
            seckillOrder.setActivityId(message.getActivityId());
            seckillOrder.setProductType(message.getProductType());
            seckillOrder.setProductId(message.getProductId());
            seckillOrder.setSeckillPrice(Math.toIntExact(message.getPrice()));
            seckillOrder.setOrderStatus(OrderStatusEnum.PENDING_PAY);
            seckillOrderMapper.insert(seckillOrder);

            // 5. 处理完成：删除幂等性key
            stringRedisTemplate.delete(orderExistKey);
            log.info("秒杀订单{}处理成功", message.getOrderNo());

        } catch (Exception e) {
            // 异常回滚：恢复Redis库存+数据库库存+删除幂等key
            stringRedisTemplate.opsForValue().increment("SECKILL:ACTIVITY:STOCK:" + message.getActivityId(), 1);
            seckillActivityMapper.increaseStock(message.getActivityId()); // 回滚DB库存
            stringRedisTemplate.delete(orderExistKey);
            log.error("处理秒杀订单{}失败", message.getOrderNo(), e);
            throw e; // 触发死信队列，避免重复重试
        }
    }
}
