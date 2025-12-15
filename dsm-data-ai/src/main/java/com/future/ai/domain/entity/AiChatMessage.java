package com.future.ai.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

// 消息实体
@Data
public class AiChatMessage {
    private Long id;
    private Long sessionId;
    private Integer messageType;        // 消息类型(1:用户,2:AI)
    private String content;             // 消息内容
    private LocalDateTime sendTime;     // 发送时间
}