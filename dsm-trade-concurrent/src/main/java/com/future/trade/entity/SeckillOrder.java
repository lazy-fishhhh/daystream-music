package com.future.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.future.trade.entity.enums.OrderStatusEnum;
import com.future.trade.entity.enums.ProductTypeEnum;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体类
 * 对应表：seckill_order
 */
@Data
@TableName("seckill_order")
public class SeckillOrder {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号（唯一标识，格式：SECKILL + 时间戳 + 6位随机数）
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 下单用户ID（关联 user 表 id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 秒杀活动ID（关联 seckill_activity.id）
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 商品类型（与 seckill_activity.product_type 一致，避免关联查询）
     */
    @TableField("product_type")
    private ProductTypeEnum productType;

    /**
     * 商品ID（如 vip_package.id、product.id、music_album.id）
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 秒杀价格（单位：分，与活动表一致，避免价格不一致问题）
     */
    @TableField("seckill_price")
    private Integer seckillPrice;

    /**
     * 订单状态（默认待支付，关联 OrderStatusEnum 枚举）
     */
    @TableField("order_status")
    private OrderStatusEnum orderStatus = OrderStatusEnum.PENDING_PAY;

    /**
     * 支付时间（订单状态变为已完成时填充）
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 创建时间（数据库自动填充）
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间（数据库自动填充）
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}