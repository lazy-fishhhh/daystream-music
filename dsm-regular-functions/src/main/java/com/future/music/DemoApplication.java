package com.future.music;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@MapperScan({"com.future.music.mapper","com.future.login.mapper"})
// 扫描当前包 + common模块的配置包（替换为你MinioConfig的实际包路径）
@ComponentScan(basePackages = {
        "com.future.music",  // 当前启动模块的根包
        "config",// dsm-common模块中config包的路径
		"com.future.login",
        "annotation"// dsm-common模块中annotation包的路径
})
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
