package com.future.trade.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * VipPackageEnum 业务枚举 ， 需关闭 MybatisPlus 枚举自动映射数据库字段功能
 * VIP套餐 枚举类（对应数据库 vip_package 表的核心套餐配置）
 * 注：此处枚举适用于数据库中预定义的基础套餐，如需动态新增套餐，需通过数据库维护
 */
@Getter
@AllArgsConstructor
public enum VipPackageEnum {

    /**
     * 月度套餐（基础类型）
     */
    MONTH(
            1,                  // 套餐ID
            VipPackageNameEnum.VIP_ONE_MONTH,         // 套餐名称
            VipPackageTypeEnum.BASIC, // 套餐类型（基础）
            30,                 // 原价
            25,                 // 现价2
            30,                 // 有效期（天）
            1                   // VIP等级（1-基础）
    ),

    /**
     * 季度套餐（基础类型）
     */
    SEASON(
            2,
            VipPackageNameEnum.VIP_THREE_MONTHS,
            VipPackageTypeEnum.BASIC,
            80,
            68,
            90,
            1
    ),

    /**
     * 年度套餐（基础类型）
     */
    YEAR(
            3,
            VipPackageNameEnum.VIP_ONE_YEAR,
            VipPackageTypeEnum.BASIC,
            240,
            198,
            365,
            1
    );

    // 套餐ID（对应数据库 vip_package.id）
    private final Integer id;

    // 套餐名称（关联套餐名称枚举，对应数据库 vip_package.package_name）
    private final VipPackageNameEnum packageName;

    // 套餐类型（关联套餐类型枚举，对应数据库 vip_package.package_type）
    private final VipPackageTypeEnum packageType;

    // 原价（对应数据库 vip_package.original_price）
    private final Integer originalPrice;

    // 现价（对应数据库 vip_package.current_price）
    private final Integer currentPrice;

    // 有效期（天，对应数据库 vip_package.duration_days）
    private final Integer durationDays;

    // VIP等级（对应数据库 vip_package.level）
    private final Integer level;


    /**
     * 根据套餐ID获取枚举实例
     * 用于从数据库查询到 package_id 后转换为枚举
     * @param id 套餐ID（数据库 vip_package.id）
     * @return 对应的枚举实例，无匹配时返回空
     */
    public static Optional<VipPackageEnum> getById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(packageEnum -> packageEnum.id.equals(id))
                .findFirst();
    }

    /**
     * 根据套餐名称（数据库存储值）获取枚举实例
     * 用于前端传入名称时定位套餐（匹配数据库存储的枚举值，如"VIP一个月"）
     * @param packageName 套餐名称（数据库 vip_package.package_name 的值）
     * @return 对应的枚举实例，无匹配时返回空
     */
    public static Optional<VipPackageEnum> getByPackageName(String packageName) {
        if (packageName == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                // 比较嵌套枚举的数据库存储值（code字段）
                .filter(packageEnum -> packageEnum.packageName.getCode().equals(packageName))
                .findFirst();
    }

    /**
     * 校验价格合理性（现价 <= 原价，且价格非负）
     * 用于套餐配置校验
     * @return 价格是否合理
     */
    public boolean isPriceValid() {
        return originalPrice >= 0
                && currentPrice >= 0
                && currentPrice <= originalPrice;
    }

    /**
     * 计算优惠金额
     * @return 原价 - 现价（单位：元），若价格无效则返回0
     */
    public Integer getDiscountAmount() {
        if (!isPriceValid()) {
            return 0;
        }
        return originalPrice - currentPrice;
    }

    /**
     * 计算优惠折扣（保留一位小数）
     * @return 折扣比例（如8.5表示85折），若原价为0则返回0.0
     */
    public double getDiscountRate() {
        if (originalPrice <= 0 || !isPriceValid()) {
            return 0.0;
        }
        // 四舍五入保留一位小数
        return Math.round(((double) currentPrice / originalPrice) * 10) / 10.0;
    }
}