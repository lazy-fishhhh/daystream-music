package com.future.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // 标题生成相关配置
    public static final String TITLE_GENERATOR_EXCHANGE = "title-generator-exchange";
    public static final String TITLE_GENERATOR_QUEUE = "title-generator-queue";
    public static final String TITLE_GENERATOR_ROUTING_KEY = "title.generate";

    // 1. 定义交换机
    @Bean
    public Exchange titleGeneratorExchange() {
        return ExchangeBuilder.directExchange(TITLE_GENERATOR_EXCHANGE)
                .durable(true)
                .build();
    }

    // 2. 定义队列
    @Bean
    public Queue titleGeneratorQueue() {
        return QueueBuilder.durable(TITLE_GENERATOR_QUEUE)
                .build();
    }

    // 3. 定义绑定关系
    @Bean
    public Binding titleGeneratorBinding() {
        return BindingBuilder.bind(titleGeneratorQueue())
                .to(titleGeneratorExchange())
                .with(TITLE_GENERATOR_ROUTING_KEY)
                .noargs();
    }

}
