package com.future.trade.entity.message;

import com.baomidou.mybatisplus.annotation.TableField;
import com.future.trade.entity.enums.OrderStatusEnum;
import com.future.trade.entity.enums.ProductTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀订单消息体
 */
@Data
public class SeckillOrderMsg implements Serializable {
    private Long activityId; // 秒杀活动ID
    private Long userId;     // 用户ID
    private Long orderNo;    // 订单号
    private Long price;      // 价格
    private ProductTypeEnum productType;
    private Long productId;
    private Integer seckillPrice;
    private OrderStatusEnum orderStatus = OrderStatusEnum.PENDING_PAY;
    private LocalDateTime payTime;
}