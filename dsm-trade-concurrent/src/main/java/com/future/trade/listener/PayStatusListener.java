package com.future.trade.listener;


import com.future.trade.service.IVipCoreService;
import com.future.trade.service.IVipOrderService;
import constants.TradeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayStatusListener {
    private final IVipOrderService vipOrderService;

    // 监听支付状态变更队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.pay.queue", durable = "true"),
            exchange = @Exchange(name = "pay.exchange", type = ExchangeTypes.TOPIC),
            key = "pay.success"
    ))
    public void listenOrderPayStatus(Long orderId) {
        // 处理支付状态变更逻辑
        System.out.println("处理支付状态变更逻辑");
        System.out.println("订单支付成功，订单ID：" + orderId);
    }
}
