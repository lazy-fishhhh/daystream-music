package config;


import ch.qos.logback.classic.pattern.MessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MqConfig implements ApplicationContextAware {
    // RabbitMQ消息转换器
    // 发消息时将java对象转换成json
    // 收消息时将json转换成java对象
    @Bean
    public MessageConverter messageConverter() {
        return new MessageConverter();
    }

    // 处理 RabbitMQ 中 “无法路由的消息” 的回调通知
    // 简单来说，就是当你发送一条消息，但 RabbitMQ 找不到任何符合条件的队列来接收它时，
    // 这条消息会被 “返回” 给生产者，而这段代码就是用来捕获并处理这个 “返回” 事件的。
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 从ApplicationContext中获取RabbitTemplate
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        // 配置回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback(){
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                // 处理返回的消息
                log.warn("收到消息的return callback: exchange:{}, routingKey:{}, message:{}, replyCode:{}, replyText:{}",
                        returnedMessage.getExchange(), returnedMessage.getRoutingKey(),
                        returnedMessage.getMessage(), returnedMessage.getReplyCode(),
                        returnedMessage.getReplyText());
            }
        });
    }
}
