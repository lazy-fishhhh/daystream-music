package com.future.ai.service;

import constants.AiConstants;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(
        wiringMode = dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel")
public interface TitleGeneratorService {

    @SystemMessage(AiConstants.TITLE_GENERATOR_PROMPT)
    String generateTitle(@UserMessage String userQuestion);
}