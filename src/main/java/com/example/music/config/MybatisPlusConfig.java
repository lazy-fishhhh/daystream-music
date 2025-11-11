package com.example.music.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();


        // 分页插件 - 必须放在第一位
        // 因为分页插件需要优先拦截 SQL 并进行分页处理，
        // 后续插件（如乐观锁）基于分页后的 SQL 再做增强，避免顺序错误导致功能异常。

        //1. 这是 MyBatis-Plus 实现分页功能的核心插件，必须配置才能使用 MyBatis-Plus 的分页查询（如 Page 对象、IPage 接口）。
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L); // 单页分页条数限制

        //setOverflow(true)：当页码超出实际总页数时的处理策略
        //true：页码超出时, 返回实际的最后一页
        //false（默认）: 页码超出时返回空数据
        paginationInterceptor.setOverflow(true);


        // 2. 将分页插件 PaginationInnerInterceptor 添加到 MyBatis-Plus 的拦截器链中，使分页功能能够生效。
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 乐观锁插件 目前没用到
        //interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
}