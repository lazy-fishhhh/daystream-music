package com.future.music.entity.pojo.deal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VipPackage {
    private Long id;                 // 主键ID
    private String packageName;      // 套餐名称
    private BigDecimal price;        // 价格（元）
    private Integer durationDays;    // 有效期（天）
    private Integer status;          // 状态（0-下架，1-上架）
}