package com.future.ai.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * 会话实体
 */
@Data
public class AiChatSession {
    private Long id; // 对应memoryId
    private Long userId;
    private Integer sessionType;        // 会话类型(1:咨询 2:创作)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}