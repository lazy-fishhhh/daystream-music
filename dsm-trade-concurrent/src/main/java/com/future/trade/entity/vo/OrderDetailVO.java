package com.future.trade.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单详情VO（用于前端展示订单完整信息）
 */

/**
 {
    "orderNo": "1991108293222281217",
    "orderTypeDesc": "VIP订阅",
    "productName": "高级VIP3个月套餐",
    "productTypeDesc": "VIP套餐",
    "totalAmount": "128元",
    "payAmount": "98元",
    "price": "98元",
    "quantity": 1,
    "statusDesc": "已支付",
    "createdTime": "2024-11-19 15:30",
    "payTime": "2024-11-19 15:32"\
    "effectiveTime": "2024-11-19 15:30",
    "expireTime": "2024-11-19 15:30"
 }
 */

@Data
public class OrderDetailVO {

    /**
     * 订单号（唯一标识）
     */
    private String orderNo;

    /**
     * 订单类型描述（如：VIP订阅、单曲购买）
     */
    private String orderType;

    /**
     * 商品名称（如：高级VIP3个月套餐）
     */
    private String productName;


    /**
     * 总金额
     */
    private String totalAmount;

    /**
     * 实付金额
     */
    private String payAmount;

    /**
     * 商品单价
     */
    private String price;

    /**
     * 购买数量（默认1）
     */
    private Integer quantity;

    /**
     * 订单状态描述（如：已支付、待支付）
     */
    private String statusDesc;

    /**
     * 订单创建时间（格式化后，如：2024-11-19 15:30）
     */
    private LocalDateTime createdTime;

    /**
     * 支付时间（格式化后，如：2024-11-19 15:32，仅已支付订单有值）
     */
    private LocalDateTime payTime;


    private LocalDateTime effectiveTime;

    private LocalDateTime expireTime;
}