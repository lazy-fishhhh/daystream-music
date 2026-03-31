package com.future.ai.domain.dto;

import lombok.Data;

@Data
public class TitleGeneratorMessage {
    private Long sessionId;
    private String userQuestion;
}