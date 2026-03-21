package com.future.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.future.trade.entity.enums.ProductTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单详情表实体类
 */
@Data
@TableName("order_item")
public class OrderItem {

    public static ProductTypeEnum ProductType;
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId; // 关联主订单ID

    @TableField("product_type")
    private int productType; // 商品类型（1-VIP套餐，2-单曲，3-打赏）

    @TableField("product_id")
    private Long productId; // 商品ID（关联对应商品表）

    @TableField("product_name")
    private String productName; // 商品名称

    @TableField("price")
    private Integer price; // 商品单价（单位：音浪）

    @TableField("quantity")
    private Integer quantity = 1; // 购买数量（默认1）

    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;
}