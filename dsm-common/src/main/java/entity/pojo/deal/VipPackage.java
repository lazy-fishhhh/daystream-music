package entity.pojo.deal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import entity.enums.VipPackageType;
import lombok.Data;
import java.time.LocalDateTime;

// 对应数据库表 vip_package
@Data
@TableName("vip_package")
public class VipPackage {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 套餐名称（动态字符串，非枚举）
    @TableField("package_name")
    private String packageName;

    // 套餐类型（枚举，对应数据库 ENUM）
    @TableField("package_type")
    private VipPackageType packageType;

    @TableField("level")
    private Integer level; // VIP等级（1-3）

    @TableField("original_price")
    private Integer originalPrice; // 原价（单位：分，避免浮点数精度问题）

    @TableField("current_price")
    private Integer currentPrice; // 现价（单位：分）

    @TableField("duration_days")
    private Integer durationDays; // 有效期天数

    @TableField("status")
    private Integer status; // 状态（0-下架，1-上架）

    @TableField("sort_order")
    private Integer sortOrder; // 排序

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}