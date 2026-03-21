package com.future.ai.domain.entity;

import lombok.Data;

@Data // 需要lombok，没有的话自己写getter/setter
public class Message {
    private String memoryId;
    private String message;
}