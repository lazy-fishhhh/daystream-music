package com.future.ai.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.future.ai.service.ConsultantService;
import com.future.ai.service.MusicCreationService;
import com.future.login.util.SecurityUtil;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Api("AI功能接口")
@RequestMapping("/ai")
public class ChatController {

    @Resource
    private ConsultantService consultantService;

    @Resource
    private MusicCreationService musicCreationService;

    @ApiOperation("与AI咨询（SSE流式输出）")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=utf-8")
    public Flux<String> chat(
            @RequestParam String memoryId,
            @RequestParam String message) {

        // 检查用户是否已登录
        if (SecurityUtil.getCurrentUser() == null) {
            // 未登录：包装为 SSE 格式的错误信息
            return Flux.just("data: 未登录，请先登录后使用AI功能\n\n")
                    .concatWith(Flux.just("data: [DONE]\n\n")); // 标记结束
        }

        // 权限校验通过，执行原有流式逻辑并包装为 SSE 格式
        return consultantService.chat(message, memoryId)
                // 将每个文本块包装为 SSE 的 data 格式
                .map(content -> "data: " + content + "\n\n")
                // 追加结束标记（前端可据此关闭连接）
                .concatWith(Flux.just("data: [DONE]\n\n"))
                // 兜底：捕获流式处理中的异常，包装为 SSE 格式的错误信息
                .onErrorResume(e -> Flux.just(
                        "data: <div>AI处理异常：" + e.getMessage() + "</div>\n\n",
                        "data: [DONE]\n\n"
                ));
    }

    @ApiOperation("创建新会话")
    @GetMapping("/newSession")
    public Result<String> createNewSession() {
        // 使用MyBatis-Plus的雪花算法生成唯一会话ID
        long sessionId = IdWorker.getId();
        return Result.ok(String.valueOf(sessionId)); // 返回字符串类型的memoryId
    }

    @ApiOperation("创作歌曲（SSE流式输出）")
    @GetMapping(
            value = "/music/create",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=utf-8"
    )
    public Flux<String> createSong(
            @RequestParam String memoryId,
            @RequestParam String request) {

        // 1. 检查用户是否已登录
        if (SecurityUtil.getCurrentUser() == null) {
            // 未登录：包装为 SSE 格式的错误信息
            return Flux.just("data: 未登录，请先登录后使用AI功能\n\n")
                    .concatWith(Flux.just("data: [DONE]\n\n")); // 标记结束
        }

        // 2. 参数校验
        if (request == null || request.trim().isEmpty()) {
            // 参数为空：包装为 SSE 格式的提示信息 + 结束标记
            return Flux.just("data: 请告诉我你的歌曲创作需求（如主题、曲风、场景等），我会为你量身打造原创歌词！\n\n")
                    .concatWith(Flux.just("data: [DONE]\n\n"));
        }

        // 3. 权限和参数校验通过，执行流式逻辑并包装为 SSE 格式
        return musicCreationService.createMusic(request, memoryId)
                // 将每个文本块包装为 SSE 的 data 格式（核心：SSE 协议要求）
                .map(content -> "data: " + content + "\n\n")
                // 追加结束标记（前端可据此关闭 SSE 连接，避免长期挂起）
                .concatWith(Flux.just("data: [DONE]\n\n"))
                // 兜底：捕获异常并包装为 SSE 格式的错误信息
                .onErrorResume(e -> Flux.just(
                        "data: <div>AI创作异常：" + e.getMessage() + "</div>\n\n",
                        "data: [DONE]\n\n"
                ));
    }

}
