package com.future.trade.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP退款消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VipRefundMessage implements Serializable {
    private String orderNo;
    private Long userId;
    private Integer refundAmount;
    private String refundReason;
    private LocalDateTime refundTime;

    public VipRefundMessage() {
    }

    public VipRefundMessage(String orderNo, Long userId, Integer refundAmount, String refundReason) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.refundAmount = refundAmount;
        this.refundReason = refundReason;
        this.refundTime = LocalDateTime.now();
    }
}
