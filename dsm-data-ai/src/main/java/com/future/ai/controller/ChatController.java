package com.future.ai.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.future.ai.domain.entity.AiChatMessage;
import com.future.ai.domain.entity.AiChatSession;
import com.future.ai.domain.entity.Message;
import com.future.ai.service.ConsultantService;
import com.future.ai.service.MessageService;
import com.future.ai.service.MusicCreationService;
import com.future.login.util.SecurityUtil;
import dev.langchain4j.model.chat.request.ChatRequest;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Api("AI功能接口")
@RequestMapping("/ai")
public class ChatController {

    @Resource
    private ConsultantService consultantService;

    @Resource
    private MusicCreationService musicCreationService;

    @Resource
    private MessageService messageService;

    @ApiOperation("查询用户会话列表")
    @GetMapping("/sessions")
    public Result<List<AiChatSession>> getSessions(
            @RequestParam String sessionType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return messageService.getSessions(sessionType, pageNum, pageSize);
    }

    @ApiOperation("根据会话id查询消息记录")
    @GetMapping("/messages")
    public Result<List<AiChatMessage>> getMessages(
            @RequestParam String memoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return messageService.getMessage(memoryId, pageNum, pageSize);
    }

    @DeleteMapping("/messages")
    @ApiOperation("删除会话")
    public Result<Void> deleteMessages(@RequestParam String memoryId) {
        return messageService.deleteMessages(memoryId);
    }

    @ApiOperation("与AI咨询")
// 1. 修改produces为SSE标准格式：text/event-stream
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=utf-8")
// 2. 用@RequestBody接收POST的JSON参数（替代@RequestParam，更符合POST传参规范）
    public Flux<String> chat(@RequestBody Message message) {

        if (message == null || message.getMessage() == null || message.getMessage().trim().isEmpty()) {
            return Flux.just("data: 聊天内容不能为空\n\n");
        }
        // 检查用户是否已登录
        if (SecurityUtil.getCurrentUser() == null) {
            // 未登录：返回SSE格式的错误信息（必须封装成data: xxx\n\n）
            return Flux.just("data: 未登录，请先登录后使用AI功能\n\n");
        }

        // 权限校验通过，执行原有流式逻辑
        return consultantService.chat(message.getMessage(), message.getMemoryId())
                // 3. 对每个流式返回的内容封装成SSE格式
                .map(content -> "data: " + content + "\n\n")
                // 兜底：捕获流式处理中的异常，封装成SSE格式
                .onErrorResume(e -> Flux.just("data: <div>AI处理异常：" + e.getMessage() + "</div>\n\n"));
    }

    @ApiOperation("创建新会话")
    @GetMapping("/newSession")
    public Result<String> createNewSession() {
        // 使用MyBatis-Plus的雪花算法生成唯一会话ID
        long sessionId = IdWorker.getId();
        return Result.ok(String.valueOf(sessionId)); // 返回字符串类型的memoryId
    }

    @ApiOperation("创作歌曲")
    @PostMapping(value = "/music/create", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=utf-8")
    public Flux<String> createSong(@RequestBody Message message) {

        // 1. 检查用户是否已登录
        if (SecurityUtil.getCurrentUser() == null) {
            // 未登录：直接返回错误Flux，不执行后续流式逻辑
            return Flux.just("data: 未登录，请先登录后使用AI功能\n\n");
        }

        if (message == null || message.getMessage() == null || message.getMessage().trim().isEmpty()) {
            return Flux.just("data: 聊天内容不能为空\n\n");
        }

        // 3. 权限和参数校验通过
        return musicCreationService.createMusic(message.getMessage(), message.getMemoryId())
                // 3. 对每个流式返回的内容封装成SSE格式
                .map(content -> "data: " + content + "\n\n")
                // 兜底：捕获流式处理中的异常，封装成SSE格式
                .onErrorResume(e -> Flux.just("data: <div>AI处理异常：" + e.getMessage() + "</div>\n\n"));
    }

}
