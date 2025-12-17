package config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MqConfig {

    // ========== 1. 定义JSON消息转换器==========
    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }

    // ========== 2. 配置RabbitTemplate ==========
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 绑定发送端转换器
        rabbitTemplate.setMessageConverter(jacksonMessageConverter());
        // 发布确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("发布确认回调ack：消息成功投递到交换机, id: {}", correlationData != null ? correlationData.getId() : "未知");
            } else {
                log.error("发布确认回调nack：消息投递到交换机失败, id: {}, 原因: {}", correlationData != null ? correlationData.getId() : "未知", cause);
            }
        });
        // 路由失败回调（你的原有逻辑）
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.warn("消息路由失败，请排查原因 :{}, routingKey:{}, replyCode:{}, replyText:{}",
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey(),
                    returnedMessage.getReplyCode(), returnedMessage.getReplyText());
        });
        return rabbitTemplate;
    }

}