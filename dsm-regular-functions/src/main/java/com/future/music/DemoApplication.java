package com.future.music;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.web.bind.annotation.*;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@RestController
@MapperScan({
        "com.future.login.mapper",
        "com.future.music.mapper",
        "com.future.ai.mapper",
            "social.mapper",
            "com.future.trade.mapper",
            "com.future.player.mapper",
			"admin.mapper"})

@ComponentScan(basePackages = {
        "com.future.common.websocket",
        "com.future.login",
        "com.future.ai" ,
        "com.future.music",
        "com.future.trade.service",
        "com.future.trade.controller",
        "com.future.trade.listener",
        "com.future.player",
        "social",
        "config",
        "annotation",
        "util",
		"admin",
        "com.future.trade.config"
})
@EnableScheduling // 启用Spring定时任务机制
@EnableRabbit // 启用Spring RabbitMQ功能
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/**
	 * 配置消息转换器，使用JSON格式序列化消息
	 * 这对于@RabbitListener声明的交换机和队列正常工作非常重要
	 */
	@Bean
	public MessageConverter messageConverter() {
		return new Jackson2JsonMessageConverter();
	}

}




