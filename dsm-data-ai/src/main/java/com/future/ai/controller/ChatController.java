package com.future.ai.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.future.ai.service.ConsultantService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Resource
    private ConsultantService consultantService;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId, String message){
        if (StringUtils.hasText(memoryId) || StringUtils.hasText(message)) {
            return Flux.error(new IllegalArgumentException("memoryId和message不能为空"));
        }
        return consultantService.chat(message, memoryId);
    }

    @RequestMapping("/newSession")
    public Mono<String> createNewSession() {
        // 使用MyBatis-Plus的雪花算法生成唯一会话ID
        long sessionId = IdWorker.getId();
        return Mono.just(String.valueOf(sessionId)); // 返回字符串类型的memoryId
    }

}
