package entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;
import java.util.Optional;

/**
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
            "VIP1个月",         // 套餐名称
            VipPackageType.BASIC, // 套餐类型（基础）
            30,                 // 原价
            25,                 // 现价
            30,                 // 有效期（天）
            1                   // VIP等级（1-基础）
    ),

    /**
     * 季度套餐（基础类型）
     */
    SEASON(
            2,
            "VIP3个月",
            VipPackageType.BASIC,
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
            "VIP1年",
            VipPackageType.BASIC,
            240,
            198,
            365,
            1
    );

    /**
     * 高级年度套餐（高级类型）
     */
//    PREMIUM_YEAR(
//            4,
//            "高级VIP1年",
//            VipPackageType.PREMIUM,
//            360,
//            298,
//            365,
//            2
//    );

    // 套餐ID
    private final Integer id;
    // 套餐名称（展示用）
    private final String packageName;
    // 套餐类型（基础/高级/超级，关联 VipPackageType 枚举）
    private final VipPackageType packageType;
    // 原价
    private final Integer originalPrice;
    // 现价
    private final Integer currentPrice;
    // 有效期（天）
    private final Integer durationDays;
    // VIP等级（1-基础，2-高级，3-超级）
    private final Integer level;



    //Optional<T> 是一个容器类，用于封装可能为 null 的对象，
    // 其核心目的是避免 NullPointerException（空指针异常），
    // 并通过语义化的 API 增强代码的可读性和健壮性


    /**
     * 根据套餐ID获取枚举实例
     * 用于从数据库查询到 package_id 后转换为枚举
     * @param id 套餐ID（数据库 vip_package.id）
     * @return 对应的枚举实例，无匹配时返回空
     */
    public static Optional<VipPackageEnum> getById(Integer id) {
        return Arrays.stream(values())
                .filter(packageEnum -> packageEnum.id.equals(id))
                .findFirst();
    }

    /**
     * 根据套餐名称获取枚举实例（模糊匹配）
     * 用于前端传入名称时定位套餐
     * @param packageName 套餐名称（如"VIP1个月"）
     * @return 对应的枚举实例，无匹配时返回空
     */
    public static Optional<VipPackageEnum> getByPackageName(String packageName) {
        if (packageName == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(packageEnum -> packageEnum.packageName.contains(packageName))
                .findFirst();
    }

    /**
     * 校验价格合理性（现价 <= 原价）
     * 用于套餐配置校验
     * @return 价格是否合理
     */
    public boolean isPriceValid() {
        return currentPrice <= originalPrice && currentPrice >= 0;
    }

    /**
     * 计算优惠金额
     * @return 原价 - 现价（单位：元）
     */
    public Integer getDiscountAmount() {
        return originalPrice - currentPrice;
    }

    /**
     * 计算优惠折扣（保留一位小数）
     * @return 折扣比例（如8.5表示85折）
     */
    public double getDiscountRate() {
        if (originalPrice == 0) {
            return 0.0;
        }
        return Math.round(((double) currentPrice / originalPrice) * 10) / 10.0;
    }
}