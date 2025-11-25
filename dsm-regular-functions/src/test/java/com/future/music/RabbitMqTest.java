package com.future.music;

import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class RabbitMqTest {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendMessage() {
        String queueName = "dsm_queue1";
        String message = "Hello, RabbitMQ!";
        rabbitTemplate.convertAndSend(queueName, message);
        System.out.println("消息发送成功！");
    }



    @Test
    public void testSendMessage2() {
        String queueName = "converterTest";
        Map<String, String> map = new HashMap<>();
        map.put("java", "java value");
        map.put("python", "python value");
        rabbitTemplate.convertAndSend(queueName, map);
        System.out.println("消息发送成功！");
    }

    /**
     * 测试RabbitMQ的发布者确认（Publisher Confirm）机制。
     * 该机制确保生产者发送的消息已成功被RabbitMQ交换机接收。
     *
     * @throws InterruptedException 如果等待被中断
     */
    @Test
    public void testConfirmCallback() throws InterruptedException {
        // 使用CountDownLatch来解决单元测试中异步回调的等待问题。
        // 因为消息发送和确认是异步的，不等待的话，测试方法会提前结束，无法捕获回调结果。
        // 初始化计数为1，表示我们需要等待一个事件（即确认回调）发生。
        CountDownLatch latch = new CountDownLatch(1);

        // 1. 创建CorrelationData对象
        // CorrelationData用于在发送消息和接收确认/返回回调之间建立关联。
        // 我们给每条消息一个唯一的ID（UUID），这样在回调中就能明确知道是哪条消息的结果。
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        log.info("准备发送消息，消息ID: {}", correlationData.getId());

        // 2. 为CorrelationData添加异步确认回调
        // 这里使用Java 8的CompletableFuture.whenComplete()方法来注册一个回调函数。
        // 当RabbitMQ服务器对这条消息做出确认（ack）或否定确认（nack）时，这个回调就会被触发。
        correlationData.getFuture().whenComplete((confirm, throwable) -> {
            // Lambda表达式的两个参数：
            // - confirm: 当确认成功时，此参数包含确认信息（CorrelationData.Confirm对象）
            // - throwable: 当发生异常（如连接失败、交换机不存在）时，此参数包含异常信息

            if (throwable != null) {
                // 情况一：发生异常，消息确认失败
                // 这通常意味着消息根本没有到达RabbitMQ服务器。
                log.error("消息确认失败nack for ID: {}: {}", correlationData.getId(), throwable.getMessage(), throwable);
            } else {
                // 情况二：成功收到RabbitMQ的确认
                // confirm.isAck() 返回true表示消息已被交换机成功接收。
                // 注意：这仅代表交换机收到了消息，不代表消息已被路由到队列或被消费者处理。
                log.info("消息确认成功ack for ID: {}, Ack Status: {}", correlationData.getId(), confirm.isAck());
            }

            // 非常重要：无论成功还是失败，都必须调用countDown()
            // 这会将CountDownLatch的计数减1，从1变为0，从而释放等待的主线程。
            latch.countDown();
            log.info("CountDownLatch计数已减1，当前计数: {}", latch.getCount());
        });

        // 3. 发送消息
        // 使用RabbitTemplate的convertAndSend方法发送消息。
        // 最后一个参数是我们创建的correlationData对象，这是将消息与回调关联起来的关键。
        rabbitTemplate.convertAndSend(
                "dsmTest_exchange",  // 目标交换机的名称
                "red777",               // 发送消息时使用的路由键
                "testConfirmCallback", // 要发送的消息内容
                correlationData      // 关联数据，用于后续的确认回调
        );
        log.info("消息已发送，等待RabbitMQ的确认...");

        // 4. 等待回调完成
        // 调用latch.await()会使当前测试线程（主线程）阻塞，直到：
        // a) 计数变为0（即回调中调用了countDown()），或者
        // b) 等待时间超过10秒。
        // 这确保了我们有足够的时间等待异步确认回调的执行。
        boolean awaitResult = latch.await(10, TimeUnit.SECONDS);

        if (awaitResult) {
            log.info("等待成功，确认回调已执行。");
        } else {
            log.warn("等待超时！在10秒内未收到RabbitMQ的确认回调。");
        }
    }


    @Test
    public void testCustomReturnCallback() throws InterruptedException {

        Map<String, String> map = new HashMap<>();
        map.put("java", "java value");
        map.put("python", "python value");
        rabbitTemplate.convertAndSend("converterTest", map);
        // 延长休眠时间到 5 秒，确保消费者线程执行完成
        Thread.sleep(5000);
    }



    @Test
    public void testId() throws InterruptedException {
        String queueName = "simple.queue";
        String message = "test id";
                rabbitTemplate.convertAndSend(queueName, message);
    }



    @Test
    public void testSendDelayMessage() throws InterruptedException {
        rabbitTemplate.convertAndSend("delay.direct", "hi", "Hello!!!", new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelayLong(10000L);
                return message;
            }
        });
        log.info("延迟消息发送消息成功！");
    }
}