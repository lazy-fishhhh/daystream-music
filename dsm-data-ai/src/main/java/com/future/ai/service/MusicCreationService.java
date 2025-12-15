package com.future.ai.service;

import constants.AiConstants;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "createChatMemoryProvider")
public interface MusicCreationService {

    // 系统提示：强约束回答格式+功能边界
    @SystemMessage(AiConstants.MUSIC_CREATION_PROMPT)
    Flux<String> createMusic(
            @UserMessage String userRequest,  // 用户的创作需求（如“写一首关于毕业的民谣”）
            @MemoryId String memoryId         // 多轮创作记忆（如用户后续修改需求，保持连贯性）
    );
}
