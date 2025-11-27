package config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 消费者消息失败处理机制： RepublishMessageRecoverer，
 * 失败后将消息投递到一个指定的，专门存放异常消息的队列（这里是error.queue），后续由人工集中处理。
 *
 * 消费者处理消息时发生异常
 * 重试机制触发后，重试次数耗尽；
 * RepublishMessageRecoverer 将失败消息重发布到 error.direct 交换机，
 * 最终路由到 error.queue 队列
 * 最后人工处理这些失败消息，例如记录日志、发送告警通知等。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq.listener.simple.retry", name = "enabled", havingValue = "true")
public class MqErrorConfig {

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange("error.direct");
    }

    @Bean
    public Queue errorQueue() {
        return new Queue("error.queue");
    }

    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange exchange) {
        return BindingBuilder.bind(errorQueue).to(exchange).with("error");
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        log.warn("消息确认失败，已将消息重新发布到 error.queue 队列");
        return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", "error");
    }

}
