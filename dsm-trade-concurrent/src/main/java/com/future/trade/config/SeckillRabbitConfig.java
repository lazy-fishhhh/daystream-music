package com.future.trade.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
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
    // ========== 配置监听容器工厂，绑定JSON转换器 ==========
    // 注意：方法名要和监听器中指定的 containerFactory 名称一致！
    //  RabbitConstants.SECKILL_RABBIT_LISTENER_CONTAINER_FACTORY
    // 该常量的值是 "seckillRabbitListenerContainerFactory"，则方法名对应这个值
    @Bean("seckillRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory seckillRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 绑定连接工厂
        factory.setConnectionFactory(connectionFactory);
        // 核心：给监听容器绑定JSON转换器
        factory.setMessageConverter(jacksonMessageConverter);
        // 可选：补充容器工厂其他配置（如并发数、重试等）
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false); // 异常消息不重新入队（避免死循环）
        return factory;
    }
}