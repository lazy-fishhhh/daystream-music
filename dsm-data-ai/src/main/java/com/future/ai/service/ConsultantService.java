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
        chatMemoryProvider = "consultantChatMemoryProvider")
public interface ConsultantService {

    @SystemMessage(AiConstants.AI_ASSISTANT_PROMPT)
    Flux<String> chat(@UserMessage String message, @MemoryId String memoryId);
}
