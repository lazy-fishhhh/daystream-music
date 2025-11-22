package com.future.music.listeners;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
public class SpringRabbitListener {
    @RabbitListener(queues = "dsm_queue1")
    public void q1MessageListener(String message) {
        System.out.println("SpringRabbitListener: " + message);
    }
}
