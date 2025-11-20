package com.future.trade.entity.enums;

import ch.qos.logback.core.model.INamedModel;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * VIP套餐名称枚举（对应vip_package.package_name）
 */
@Getter
public enum VipPackageNameEnum {
    VIP_ONE_MONTH(1,"VIP一个月", "一个月VIP套餐"),
    VIP_THREE_MONTHS(2, "VIP三个月", "三个月VIP套餐"),
    VIP_ONE_YEAR(3, "VIP一年", "一年VIP套餐");

    private final int id;
    @EnumValue // 标记数据库存储的枚举值
    private final String code;
    private final String desc; // 套餐描述（可选，用于业务展示）
    // 构造方法
    VipPackageNameEnum(int id, String code, String desc) {
        this.id = id;
        this.code = code;
        this.desc = desc;
    }

    public static VipPackageNameEnum getById(int id) {
        for (VipPackageNameEnum value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return null;
    }

    public static Integer getIdByCode(String code) {
        for (VipPackageNameEnum value : values()) {
            if (value.code.equals(code)) {
                return value.id;
            }
        }
        return null;
    }
}