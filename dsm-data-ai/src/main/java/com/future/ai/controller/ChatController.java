package com.future.ai.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.future.ai.service.ConsultantService;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Resource
    private ConsultantService consultantService;

    @GetMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId, String message){
        // 检查用户是否已登录
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            // 未登录：直接返回错误Flux，不执行后续流式逻辑
            return Flux.just("未登录，请先登录后使用AI功能");
        }

        // 权限校验通过，执行原有流式逻辑
        return consultantService.chat(message, memoryId)
                // 兜底：捕获流式处理中的异常，避免响应异常
                .onErrorResume(e -> Flux.just("<div>AI处理异常：" + e.getMessage() + "</div>"));
    }

    @GetMapping("/newSession")
    public Result<String> createNewSession() {
        // 使用MyBatis-Plus的雪花算法生成唯一会话ID
        long sessionId = IdWorker.getId();
        return Result.ok(String.valueOf(sessionId)); // 返回字符串类型的memoryId
    }

}
