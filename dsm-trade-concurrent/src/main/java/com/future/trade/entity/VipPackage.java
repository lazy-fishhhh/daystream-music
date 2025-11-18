package com.future.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * VIP套餐实体类
 * 对应表: vip_package
 */
@Entity // 声明这是一个JPA实体
@Table(name = "vip_package")
@Data
@EqualsAndHashCode(callSuper = false) //callSuper = false表示生成的方法不包含父类的字段
public class VipPackage {
    
    /**
     * 套餐ID
     */
    @Id //标记当前字段为实体类的主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)//指定主键的生成策略  这里是自增
    private Long id;

    /**
     * 套餐名称
     */
    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    /**
     * 套餐类型：基础/高级/超级
     */
    @Enumerated(EnumType.STRING) //指定枚举类型的存储方式  这里是字符串
    @Column(name = "package_type", nullable = false)
    private PackageType packageType;

    /**
     * VIP等级 1-基础VIP 2-高级VIP 3-超级VIP
     */
    @Column(nullable = false)
    private Integer level;

    /**
     * 原价
     */
    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    /**
     * 现价
     */
    @Column(name = "current_price", nullable = false)
    private Integer currentPrice;


    /**
     * 有效期天数
     */
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    /**
     * 状态：0-下架 1-上架
     */
    @Column(nullable = false)
    private Integer status = 1;

    /**
     * 排序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 创建时间
     */
    @CreationTimestamp //自动将字段值设置为记录创建时的时间（仅在插入数据时生效）
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp //自动将字段值更新为记录最后修改时的时间（插入和更新时均生效），用于追踪数据变更时间
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    /**
     * 套餐类型枚举
     */
    public enum PackageType {
        BASIC("基础版"),
        PREMIUM("高级版"), 
        SUPER("超级版");

        private final String description;

        PackageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 状态枚举
     */
    public enum Status {
        DISABLED(0, "下架"),
        ENABLED(1, "上架");

        private final int code;
        private final String description;

        Status(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static Status fromCode(int code) {
            for (Status status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid status code: " + code);
        }
    }

    // 业务方法
    
    /**
     * 是否可用
     */
    @Transient
    public boolean isEnabled() {
        return Status.ENABLED.getCode() == this.status;
    }


    /**
     * 验证套餐数据有效性
     */
    public boolean isValid() {
        return packageName != null && !packageName.trim().isEmpty()
                && packageType != null
                && level != null && level >= 1 && level <= 3
                && originalPrice != null && originalPrice >= 0
                && currentPrice != null && currentPrice >= 0
                && durationDays != null && durationDays > 0
                && status != null;
    }
}