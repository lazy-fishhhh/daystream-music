package com.future.trade.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * VIP套餐类型枚举（对应package_type字段）
 */
@Getter
public enum VipPackageTypeEnum {

    BASIC("BASIC", "基础套餐"),
    PREMIUM("PREMIUM", "高级套餐"),
    SUPER("SUPER", "超级套餐"),
    SECKILL("SECKILL", "秒杀套餐");



    @EnumValue // MyBatis-Plus映射数据库存储值
    private final String code; // 数据库存储的枚举值
    private final String desc; // 中文描述（用于前端展示）

    VipPackageTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 可选：根据code获取枚举实例（用于数据库查询后转换）
    public static VipPackageTypeEnum getByCode(String code) {
        for (VipPackageTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}