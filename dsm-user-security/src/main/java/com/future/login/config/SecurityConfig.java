package com.future.login.config;

import com.future.login.filter.JwtFilter;
import com.future.login.handler.AccessDeniedHandlerImpl;
import com.future.login.handler.AuthenticationEntryPointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Autowired
    private AccessDeniedHandlerImpl accessDeniedHandler;
    @Autowired
    private AuthenticationEntryPointImpl authenticationEntryPoint;
    @Autowired
    private JwtFilter jwtFilter;

    //密码加密
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // 直接从 AuthenticationConfiguration 中获取
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .logout(logout -> logout.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/ws/chat/**",
                                "/ai/chat",
                                "/ai/music/create",
                                "/resources/**",
                                "/login/*",
                                "/rejester/**").permitAll()

                        // 其他所有请求都必须认证（登录后才能访问）
                        .anyRequest().authenticated()

                )
                .csrf(csrf -> csrf.disable()) // WebSocket 必须禁用CSRF！
                // 2. 添加自定义过滤器
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // 3. 配置认证失败处理器
                // 合并异常处理配置（关键：避免覆盖）
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint) // 未认证处理器
                        .accessDeniedHandler(accessDeniedHandler) // 权限不足处理器

                );

        return http.build();
    }
}
