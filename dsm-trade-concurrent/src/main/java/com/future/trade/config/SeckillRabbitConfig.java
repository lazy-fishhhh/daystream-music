package com.future.trade.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static constants.RabbitConstants.*;


/**
 * 秒杀模块专属MQ配置（手动确认）
 */
@Configuration
public class SeckillRabbitConfig {

    // 1. 配置秒杀订单交换机
    @Bean(name = SECKILL_ORDER_EXCHANGE)
    public Exchange seckillOrderExchange() {
        return ExchangeBuilder.topicExchange(SECKILL_ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    // 2. 配置秒杀订单队列
    @Bean(name = SECKILL_ORDER_QUEUE)
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .build();
    }

    // 3. 配置绑定关系
    @Bean
    public Binding seckillOrderBinding(@Qualifier(SECKILL_ORDER_EXCHANGE) Exchange exchange,
                                       @Qualifier(SECKILL_ORDER_QUEUE) Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_SECKILL_ORDER)
                .noargs();
    }

    // 4. 秒杀专用的手动确认工厂
    @Bean(name = SECKILL_RABBIT_LISTENER_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory seckillRabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // 核心：手动确认模式
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // 秒杀高并发优化配置
        factory.setPrefetchCount(100); // 批量预取消息
        factory.setConcurrentConsumers(5); // 初始消费线程
        factory.setMaxConcurrentConsumers(20); // 最大消费线程

        // 关闭重试（不配置默认即关闭）
        // 若要显式关闭重试，可配置空的RetryTemplate
        // factory.setRetryTemplate(new RetryTemplate());

        return factory;
    }
}