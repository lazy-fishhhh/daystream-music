package com.future.ai.config;

import com.future.ai.repository.ConsultantChatMemoryStore;
import com.future.ai.repository.CreateChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private CreateChatMemoryStore createChatMemoryStore;

    @Resource
    private ConsultantChatMemoryStore consultantChatMemoryStore;

    @Bean
    public ChatMemoryProvider createChatMemoryProvider(){
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(createChatMemoryStore)
                        .build();
            }
        };
    }

    @Bean
    public ChatMemoryProvider consultantChatMemoryProvider(){
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(consultantChatMemoryStore)
                        .build();
            }
        };
    }

}
