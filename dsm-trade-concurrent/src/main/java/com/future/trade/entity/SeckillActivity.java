package com.future.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.future.trade.entity.enums.ProductTypeEnum;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体类
 * 对应表：seckill_activity
 */
@Data
@TableName("seckill_activity")
public class SeckillActivity {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 活动名称（如：七天免费会员秒杀、月度VIP限时折扣）
     */
    @TableField("activity_name")
    private String activityName;

    /**
     * 商品类型（关联 ProductTypeEnum 枚举）
     */
    @TableField("product_type")
    private ProductTypeEnum productType;

    /**
     * 关联商品ID（根据 product_type 对应不同表：1->vip_package.id，2->product.id，3->music_album.id）
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 总库存（活动可秒杀的商品总数）
     */
    @TableField("total_stock")
    private Integer totalStock;

    /**
     * 剩余库存（实时扣减，用于判断是否还有秒杀资格）
     */
    @TableField("remaining_stock")
    private Integer remainingStock;

    /**
     * 秒杀价格（单位：音浪，默认值99999）
     */
    @TableField("seckill_price")
    private Integer seckillPrice = 99999;

    /**
     * 活动开始时间（精确到秒，开始前状态为未开始）
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 活动结束时间（精确到秒，结束后状态变为已结束）
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 活动状态（0-未开始，1-进行中，2-已结束）
     */
    @TableField("status")
    private Integer status = 0;

    /**
     * 创建时间（数据库自动填充，无需手动设置）
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间（数据库自动填充，修改时自动更新）
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}