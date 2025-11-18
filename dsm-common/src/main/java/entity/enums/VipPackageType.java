package entity.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * VIP套餐类型枚举（对应数据库 vip_package.package_type 字段）
 */
@Getter
@AllArgsConstructor
public enum VipPackageType {

    /**
     * 基础套餐
     */
    BASIC("基础套餐"),

    /**
     * 高级套餐
     */
    PREMIUM("高级套餐"),

    /**
     * 超级套餐
     */
    SUPER("超级套餐");

    /**
     * 枚举描述（用于前端展示或日志输出）
     */
    private final String description;

    /**
     * 根据枚举名称获取枚举实例（兼容数据库存储的字符串值）
     * @param name 枚举名称（如 "BASIC"）
     * @return 对应的枚举实例，若不存在则返回 null
     */
    public static VipPackageType getByName(String name) {
        if (name == null) {
            return null;
        }
        for (VipPackageType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }
}