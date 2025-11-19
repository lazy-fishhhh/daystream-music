package com.future.music;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@MapperScan({"com.future.music.mapper"
            ,"com.future.login.mapper"
            ,"social.mapper"
            ,"com.future.trade.mapper"
            ,"com.future.trade.entity"})

@ComponentScan(basePackages = {
        "com.future.music",
        "com.future.login",
        "com.future.trade.service",
        "com.future.trade.controller",
        "social",
        "config",
        "annotation"
})
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
