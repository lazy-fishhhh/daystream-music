package com.future.music.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static constants.RabbitConstants.*;

/**
 * RabbitMQ配置类：创建「创建歌单成功」相关的交换机、队列、绑定关系
 * 解决注解声明交换机/队列失效的问题，统一管理MQ资源
 */
@Configuration
public class RabbitMqPlaylistConfig {

    // 1. 创建「创建歌单成功」主题交换机
    @Bean("createPlaylistSuccessExchange") // 自定义Bean名称
    public Exchange createPlaylistSuccessExchange() {
        return ExchangeBuilder.topicExchange(CREATE_PLAYLIST_SUCCESS_TOPIC_EXCHANGE)
                .durable(true) // 持久化，MQ重启不丢失
                .build();
    }

    // 2. 创建「创建歌单成功」队列
    @Bean("createPlaylistSuccessQueue") // 自定义Bean名称，语义清晰
    public Queue createPlaylistSuccessQueue() {
        return QueueBuilder.durable(CREATE_PLAYLIST_SUCCESS_QUEUE)
                .build();
    }

    // 3. 绑定队列到交换机（指定路由键）
    @Bean
    public Binding createPlaylistSuccessBinding(
            @Qualifier("createPlaylistSuccessExchange") Exchange exchange,
            @Qualifier("createPlaylistSuccessQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_CREATE_PLAYLIST_SUCCESS) // 路由键匹配
                .noargs();
    }
}