package com.future.trade.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

// 订单状态枚举
public enum OrderStatusEnum {
    PENDING_PAY(0, "待支付"),
    COMPLETED(1, "已完成"),
    CANCELLED(2, "已取消");

    @EnumValue
    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    // getter
    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}