package com.future.music.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${redisson.singleServerConfig.address}")
    private String address;

    @Value("${redisson.singleServerConfig.password:}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress(address)
                .setPassword(password.isEmpty() ? null : password);

        // 可选：设置其他默认值
        config.useSingleServer()
                .setDatabase(0)
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(4)
                .setConnectTimeout(10000)
                .setTimeout(3000);

        config.setReferenceEnabled(false);

        return Redisson.create(config);
    }
}