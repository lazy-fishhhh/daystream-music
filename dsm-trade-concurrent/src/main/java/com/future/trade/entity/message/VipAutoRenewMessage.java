package com.future.trade.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP自动续费消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VipAutoRenewMessage implements Serializable {
    private Long userId;
    private Long vipId;
    private Long packageId;
    private LocalDateTime renewTime;

    public VipAutoRenewMessage() {
    }

    public VipAutoRenewMessage(Long userId, Long vipId, Long packageId) {
        this.userId = userId;
        this.vipId = vipId;
        this.packageId = packageId;
        this.renewTime = LocalDateTime.now();
    }
}
