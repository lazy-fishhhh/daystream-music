package com.example.music.entity.pojo.music;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Musician {
    private Long id;                 // 主键ID
    private Long userId;             // 关联用户ID
    private String stageName;        // 艺名
    private String introduction;     // 简介
    private String avatar;           // 音乐人头像
    private Integer status;          // 状态（0-待审核，1-已认证，2-已封禁）
    private Integer totalFans;       // 粉丝总数
    private BigDecimal totalIncome;  // 累计收入
    private LocalDateTime createdTime; // 创建时间
}