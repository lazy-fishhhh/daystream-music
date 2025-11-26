package com.future.trade.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP秒杀结果消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VipSeckillResultMessage implements Serializable {
    private Long userId;
    private Long activityId;
    private Boolean success;
    private String message;
    private String token;
    private LocalDateTime resultTime;

    public VipSeckillResultMessage() {
    }

    public VipSeckillResultMessage(Long userId, Long activityId, Boolean success, String message, String token) {
        this.userId = userId;
        this.activityId = activityId;
        this.success = success;
        this.message = message;
        this.token = token;
        this.resultTime = LocalDateTime.now();
    }
}
