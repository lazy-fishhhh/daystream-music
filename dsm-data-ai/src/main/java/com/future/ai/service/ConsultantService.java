package com.future.ai.service;

import constants.AiConstants;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface ConsultantService {

    @SystemMessage(AiConstants.AI_ASSISTANT_PROMPT)
    Flux<String> chat(@UserMessage String message, @MemoryId String memoryId);
}
