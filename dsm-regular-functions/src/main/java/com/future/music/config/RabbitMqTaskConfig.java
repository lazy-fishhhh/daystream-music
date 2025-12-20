package com.future.music.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static constants.RabbitConstants.*;

/**
 * RabbitMQ配置类：创建任务相关的交换机、队列、绑定关系
 * 解决注解声明交换机/队列失效的问题，统一管理MQ资源
 */
@Configuration
public class RabbitMqTaskConfig {

    // 1. 创建「更新头像成功」主题交换机
    @Bean("updateAvatarSuccessExchange")
    public Exchange updateAvatarSuccessExchange() {
        return ExchangeBuilder.topicExchange(UPDATE_AVATAR_SUCCESS_TOPIC_EXCHANGE)
                .durable(true) // 持久化，MQ重启不丢失
                .build();
    }

    // 2. 创建「更新头像成功」队列
    @Bean("updateAvatarSuccessQueue")
    public Queue updateAvatarSuccessQueue() {
        return QueueBuilder.durable(UPDATE_AVATAR_SUCCESS_QUEUE)
                .build();
    }

    // 3. 绑定队列到交换机（指定路由键）
    @Bean
    public Binding updateAvatarSuccessBinding(
            @Qualifier("updateAvatarSuccessExchange") Exchange exchange,
            @Qualifier("updateAvatarSuccessQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_UPDATE_AVATAR_SUCCESS) // 路由键匹配
                .noargs();
    }

    // 4. 创建「绑定手机号成功」主题交换机
    @Bean("bindPhoneSuccessExchange")
    public Exchange bindPhoneSuccessExchange() {
        return ExchangeBuilder.topicExchange(BIND_PHONE_SUCCESS_TOPIC_EXCHANGE)
                .durable(true) // 持久化，MQ重启不丢失
                .build();
    }

    // 5. 创建「绑定手机号成功」队列
    @Bean("bindPhoneSuccessQueue")
    public Queue bindPhoneSuccessQueue() {
        return QueueBuilder.durable(BIND_PHONE_SUCCESS_QUEUE)
                .build();
    }

    // 6. 绑定队列到交换机（指定路由键）
    @Bean
    public Binding bindPhoneSuccessBinding(
            @Qualifier("bindPhoneSuccessExchange") Exchange exchange,
            @Qualifier("bindPhoneSuccessQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_BIND_PHONE_SUCCESS) // 路由键匹配
                .noargs();
    }

    // 7. 创建「初次更新个人信息成功」主题交换机
    @Bean("updateUserInfoFirstTimeExchange")
    public Exchange updateUserInfoFirstTimeExchange() {
        return ExchangeBuilder.topicExchange(UPDATE_USER_INFO_FIRST_TIME_TOPIC_EXCHANGE)
                .durable(true) // 持久化，MQ重启不丢失
                .build();
    }

    // 8. 创建「初次更新个人信息成功」队列
    @Bean("updateUserInfoFirstTimeQueue")
    public Queue updateUserInfoFirstTimeQueue() {
        return QueueBuilder.durable(UPDATE_USER_INFO_FIRST_TIME_QUEUE)
                .build();
    }

    // 9. 绑定队列到交换机（指定路由键）
    @Bean
    public Binding updateUserInfoFirstTimeBinding(
            @Qualifier("updateUserInfoFirstTimeExchange") Exchange exchange,
            @Qualifier("updateUserInfoFirstTimeQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_UPDATE_USER_INFO_FIRST_TIME) // 路由键匹配
                .noargs();
    }

    // 10. 创建「兑换会员成功」主题交换机
    @Bean("exchangeVipSuccessExchange")
    public Exchange exchangeVipSuccessExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_VIP_SUCCESS_TOPIC_EXCHANGE)
                .durable(true) // 持久化，MQ重启不丢失
                .build();
    }

    // 11. 创建「兑换会员成功」队列
    @Bean("exchangeVipSuccessQueue")
    public Queue exchangeVipSuccessQueue() {
        return QueueBuilder.durable(EXCHANGE_VIP_SUCCESS_QUEUE)
                .build();
    }

    // 12. 绑定队列到交换机（指定路由键）
    @Bean
    public Binding exchangeVipSuccessBinding(
            @Qualifier("exchangeVipSuccessExchange") Exchange exchange,
            @Qualifier("exchangeVipSuccessQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_EXCHANGE_VIP_SUCCESS) // 路由键匹配
                .noargs();
    }
}
