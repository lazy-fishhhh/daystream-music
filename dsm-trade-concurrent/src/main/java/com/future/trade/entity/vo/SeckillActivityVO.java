package com.future.trade.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 新增秒杀商品响应DTO
 */
@Data
@Accessors(chain = true)
public class SeckillActivityVO {

    /**
     * 秒杀活动ID
     */
    private Long activityId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 商品类型
     */
    private Integer productType;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 原价
     */
    private Integer originalPrice;

    /**
     * 秒杀价
     */
    private Integer seckillPrice;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 剩余库存
     */
    private Integer remainingStock;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 状态（0-未开始，1-进行中，2-已结束）
     */
    private Integer status;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 将LocalDateTime转换为字符串格式
     */
    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
}