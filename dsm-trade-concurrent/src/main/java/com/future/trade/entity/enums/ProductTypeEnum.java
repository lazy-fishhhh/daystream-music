// ProductTypeEnum.java
package com.future.trade.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ProductTypeEnum {
    VIP_PACKAGE(1, "会员套餐"),
    PHYSICAL_GOODS(2, "实物商品"),
    VIRTUAL_GOODS(3, "虚拟商品（专辑/单曲）");

    @EnumValue
    private final Integer code;
    private final String desc;

    ProductTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProductTypeEnum getByCode(Integer code) {
        for (ProductTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }


    // getter
    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}