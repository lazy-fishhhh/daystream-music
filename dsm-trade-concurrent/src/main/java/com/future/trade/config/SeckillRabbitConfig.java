package com.future.trade.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static constants.RabbitConstants.*;

@Configuration
public class SeckillRabbitConfig {

    // 1. 秒杀订单交换机
    @Bean(name = SECKILL_ORDER_EXCHANGE)
    public Exchange seckillOrderExchange() {
        return ExchangeBuilder.topicExchange(SECKILL_ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    // 2. 秒杀订单队列（绑定死信）
    @Bean(name = SECKILL_ORDER_QUEUE)
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .deadLetterExchange("seckill.dead.exchange") // 死信交换机
                .deadLetterRoutingKey("seckill.dead") // 死信路由键
                .ttl(60000) // 消息过期时间
                .build();
    }

    // 3. 绑定关系
    @Bean
    public Binding seckillOrderBinding(@Qualifier(SECKILL_ORDER_EXCHANGE) Exchange exchange,
                                       @Qualifier(SECKILL_ORDER_QUEUE) Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_SECKILL_ORDER)
                .noargs();
    }

    // 4. 死信交换机+队列+绑定（新增）
    @Bean("seckill.dead.exchange")
    public Exchange seckillDeadExchange() {
        return ExchangeBuilder.topicExchange("seckill.dead.exchange").durable(true).build();
    }

    @Bean("seckill.dead.queue")
    public Queue seckillDeadQueue() {
        return QueueBuilder.durable("seckill.dead.queue").build();
    }

    @Bean
    public Binding seckillDeadBinding(@Qualifier("seckill.dead.exchange") Exchange exchange,
                                      @Qualifier("seckill.dead.queue") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("seckill.dead").noargs();
    }

    // 5. 秒杀容器工厂（核心修复：内置转换器+关闭自动ACK）
    @Bean("seckillRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory seckillRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // 内置创建转换器，避免注入失败
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false);
        // 关键：关闭自动ACK，启用手动ACK
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}