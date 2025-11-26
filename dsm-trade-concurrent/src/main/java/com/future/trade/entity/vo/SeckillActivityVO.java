package com.future.trade.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

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
     * 状态（0-未开始，1-进行中，2-已结束）
     */
    private Integer status;

    /**
     * 提示信息
     */
    private String message;
}