package com.future.trade.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static constants.RabbitConstants.*;


/**
 * 秒杀模块专属MQ配置（手动确认）
 */
@Configuration
public class SeckillRabbitConfig {

    // 1. 秒杀订单交换机（Topic类型）
    @Bean
    public TopicExchange seckillOrderExchange() {
        return new TopicExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    // 2. 秒杀订单队列（持久化）
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .withArgument("x-queue-mode", "lazy") // 懒加载，适配高并发
                .build();
    }

    // 3. 强制绑定交换机和队列（路由键100%匹配）
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(ROUTING_KEY_SECKILL_ORDER);
    }

    // 4. 手动确认工厂
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