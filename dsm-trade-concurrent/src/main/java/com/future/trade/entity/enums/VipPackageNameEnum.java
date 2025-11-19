package com.future.trade.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * VIP套餐名称枚举（对应vip_package.package_name）
 */
@Getter
public enum VipPackageNameEnum {
    VIP_ONE_MONTH("VIP一个月", "一个月VIP套餐"),
    VIP_THREE_MONTHS("VIP三个月", "三个月VIP套餐"),
    VIP_ONE_YEAR("VIP一年", "一年VIP套餐");

    @EnumValue // 标记数据库存储的枚举值
    private final String code;
    private final String desc; // 套餐描述（可选，用于业务展示）

    VipPackageNameEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}