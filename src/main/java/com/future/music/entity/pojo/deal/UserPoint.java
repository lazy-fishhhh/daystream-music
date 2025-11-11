package com.future.music.entity.pojo.deal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPoint {
    private Long id;                 // 主键ID
    private Long userId;             // 用户ID
    private Integer pointBalance;    // 当前积分余额
    private LocalDateTime updatedTime; // 更新时间
}