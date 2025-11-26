package com.future.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface ConsultantService {
    public Flux<String> chat(@UserMessage String message, @MemoryId String memoryId);
}
