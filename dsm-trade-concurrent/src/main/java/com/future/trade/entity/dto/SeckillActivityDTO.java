package com.future.trade.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 新增秒杀商品请求DTO
 */
@Data
public class SeckillActivityDTO {

    /**
     * 活动名称（非空，长度1-200）
     */
    @NotBlank(message = "活动名称不能为空")
    @Size(min = 1, max = 200, message = "活动名称长度必须在1-200字之间")
    private String activityName;

    /**
     * 商品类型（1-会员套餐，2-实物商品，3-虚拟商品）
     */
    @NotNull(message = "商品类型不能为空")
    @Min(value = 1, message = "商品类型只能是1、2、3")
    @Max(value = 3, message = "商品类型只能是1、2、3")
    private Integer productType;

    /**
     * 关联商品ID（必须存在对应商品表中）
     */
    @NotNull(message = "商品ID不能为空")
    @Positive(message = "商品ID必须为正整数")
    private Long productId;

    /**
     * 总库存（必须>0，且≤100000，防止超大数据量）
     */
    @NotNull(message = "总库存不能为空")
    @Positive(message = "总库存必须大于0")
    @Max(value = 100000, message = "总库存不能超过100000")
    private Integer totalStock;

    /**
     * 秒杀价格（单位：分，≥0，免费则填0）
     */
    @NotNull(message = "秒杀价格不能为空")
    @Min(value = 0, message = "秒杀价格不能为负数")
    private Integer seckillPrice;

    /**
     * 活动开始时间（必须晚于当前时间）
     */
    @NotNull(message = "活动开始时间不能为空")
    @Future(message = "活动开始时间必须晚于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 活动结束时间（必须晚于开始时间，且活动时长≤7天）
     */
    @NotNull(message = "活动结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}