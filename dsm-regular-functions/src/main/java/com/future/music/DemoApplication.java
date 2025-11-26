package com.future.music;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@MapperScan({"com.future.music.mapper"
            ,"com.future.login.mapper"
            ,"social.mapper"
            ,"com.future.trade.mapper"})

@ComponentScan(basePackages = {
        "com.future.ai" ,
        "com.future.music",
        "com.future.login",
        "com.future.trade.service",
        "com.future.trade.controller",
        "social",
        "config",
        "annotation",
        "util"
})
@EnableScheduling // 启用Spring定时任务机制
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}




