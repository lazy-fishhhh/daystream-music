package com.future.ai.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.future.ai.domain.entity.AiChatMessage;
import com.future.ai.service.ConsultantService;
import com.future.ai.service.MessageService;
import com.future.ai.service.MusicCreationService;
import com.future.login.util.SecurityUtil;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
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

    @ApiOperation("根据会话id查询消息记录")
    @GetMapping("/messages")
    public Result<List<AiChatMessage>> getMessages(
            @RequestParam String memoryId,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        return messageService.getMessage(memoryId, pageNum, pageSize);
    }

    @DeleteMapping("/messages")
    @ApiOperation("删除会话")
    public Result<Void> deleteMessages(@RequestParam String memoryId) {
        return messageService.deleteMessages(memoryId);
    }

    @ApiOperation("与AI咨询")
    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(@RequestParam String memoryId, @RequestParam String message){
        // 检查用户是否已登录
        if (SecurityUtil.getCurrentUser() == null) {
            // 未登录：直接返回错误Flux，不执行后续流式逻辑
            return Flux.just("未登录，请先登录后使用AI功能");
        }

        // 权限校验通过，执行原有流式逻辑
        return consultantService.chat(message, memoryId)
                // 兜底：捕获流式处理中的异常，避免响应异常
                .onErrorResume(e -> Flux.just("<div>AI处理异常：" + e.getMessage() + "</div>"));
    }

    @ApiOperation("创建新会话")
    @GetMapping("/newSession")
    public Result<String> createNewSession() {
        // 使用MyBatis-Plus的雪花算法生成唯一会话ID
        long sessionId = IdWorker.getId();
        return Result.ok(String.valueOf(sessionId)); // 返回字符串类型的memoryId
    }

    @ApiOperation("创作歌曲")
    @PostMapping(value = "/music/create", produces = "text/html;charset=utf-8")
    public Flux<String> createSong(
            @RequestParam String memoryId,
            @RequestParam String request) {

        // 1. 检查用户是否已登录
        if (SecurityUtil.getCurrentUser() == null) {
            // 未登录：直接返回错误Flux，不执行后续流式逻辑
            return Flux.just("未登录，请先登录后使用AI功能");
        }

        // 2. 参数校验
        if (request == null || request.trim().isEmpty()) {
            return Flux.just("请告诉我你的歌曲创作需求（如主题、曲风、场景等），我会为你量身打造原创歌词！");
        }

        // 3. 权限和参数校验通过
        return musicCreationService.createMusic(request, memoryId)
                .onErrorResume(e -> Flux.just(
                        "<div>AI创作异常：" + e.getMessage() + "</div>"));
    }

}
