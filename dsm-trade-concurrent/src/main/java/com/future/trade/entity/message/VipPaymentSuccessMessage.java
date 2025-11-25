package com.future.trade.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP支付成功消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VipPaymentSuccessMessage implements Serializable {
    private String orderNo;
    private Long userId;
    private Long vipId;
    private Long packageId;
    private Integer amount;
    private LocalDateTime paymentTime;

    public VipPaymentSuccessMessage() {
    }

    public VipPaymentSuccessMessage(String orderNo, Long userId, Long vipId, Long packageId, Integer amount) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.vipId = vipId;
        this.packageId = packageId;
        this.amount = amount;
        this.paymentTime = LocalDateTime.now();
    }
}

