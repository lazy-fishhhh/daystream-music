package com.future.trade.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP状态变更消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VipStatusChangeMessage implements Serializable {
    private Long userId;
    private Integer oldStatus;
    private Integer newStatus;
    private LocalDateTime changeTime;
    private String reason;
    
    public VipStatusChangeMessage() {}
    
    public VipStatusChangeMessage(Long userId, Integer oldStatus, Integer newStatus, String reason) {
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changeTime = LocalDateTime.now();
    }
}

