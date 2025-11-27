package com.future.music.listeners;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 这个类测试用的
 * 需要消费者监听队列就自己创建一个listener
 */
@Slf4j
@Component
public class SpringRabbitListener {

    // 使用转换器后，消息体会被转换为 Map<String, String>
    @RabbitListener(queues = "converterTest")
    public void converterTestMessageListener(Map<String, String> message) {
        System.out.println("converterTestMessageListener接收到消息: " + message);
    }



    // 声明交换机和队列
    // 使用 @QueueBinding 注解绑定队列、交换机和路由键
    // 这里的队列名、交换机名和路由键都与之前的配置保持一致
    // @QueueBinding 注解可以省略 @Queue 和 @Exchange 注解
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "dsmTest_queue2"),
            exchange = @Exchange(name = "dsmTest_exchange", type = ExchangeTypes.DIRECT),
            key = {"red", "green"}
    ))
    public void q2MessageListener(String message) {
        System.out.println("SpringRabbitListener: " + message);
    }

    @RabbitListener(queues = "test.queue")
    public void errorMessageListener(String message) {
        System.out.println("接收到了消费者的消息: " + message);
        throw new RuntimeException("测试异常");
    }

//    @RabbitListener(queues = "simple.queue")
    public void testMessageListener(String message) {
        System.out.println("simple.queue接收到消息: " + message);
    }

    // 基于插件的延迟队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "delay.queue", durable = "true"),
            exchange = @Exchange(name = "delay.direct", delayed = "true"),
            key = "hi"
    ))
    public void listenDelayQueue(String message) {
        log.info("接收到delay.queue消息: " + message);
    }

}
