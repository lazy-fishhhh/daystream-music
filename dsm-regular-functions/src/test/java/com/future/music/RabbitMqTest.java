package com.future.music;

import org.junit.Test;

import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@SpringBootTest(classes = DemoApplication.class)
@RunWith(SpringRunner.class) // 确保使用 SpringRunner 运行测试
public class RabbitMqTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Test
    public void testSendMessage() {
        String queueName = "huayue-queue1";
        String message = "Hello, RabbitMQ!";
        rabbitTemplate.convertAndSend(queueName, message);
    }
}
