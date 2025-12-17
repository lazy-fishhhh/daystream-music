package config;


import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MqConfig  {



    // RabbitMQ消息转换器
    // 发消息时将java对象转换成json
    // 收消息时将json转换成java对象
    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }


    // 生产者确认机制-高并发下显著影响性能，尽量避免使用。初学的话用着练练
    // 处理 RabbitMQ 中 “无法路由的消息” 的回调通知
    // 简单来说，就是当你发送一条消息，但 RabbitMQ 找不到任何符合条件的队列来接收它时，
    // 这条消息会被 “返回” 给生产者，而这段代码就是用来捕获并处理这个 “返回” 事件的。
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 设置消息转换器
        rabbitTemplate.setMessageConverter(jacksonMessageConverter());

        // 设置 ConfirmCallback
        // ConfirmCallback 是 发布确认回调，它的作用是确认消息是否成功投递到交换机（Exchange）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("发布确认回调ack：消息成功投递到交换机, id: {}", correlationData != null ? correlationData.getId() : "未知");
            } else {
                log.error("发布确认回调nack：消息投递到交换机失败, id: {}, 原因: {}", correlationData != null ? correlationData.getId() : "未知", cause);
                // 在这里处理消息丢失的情况
            }
        });

        // 设置 ReturnsCallback
        // ReturnsCallback 是 发布返回回调，它的作用是处理消息到达交换机后路由失败的情况
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.warn("消息路由失败，请排查原因 :{}, routingKey:{}, replyCode:{}, replyText:{}",
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey(),
                    returnedMessage.getReplyCode(), returnedMessage.getReplyText());
            // 在这里可以添加更复杂的处理逻辑，比如记录到数据库或触发告警
        });

        return rabbitTemplate;
    }
}
