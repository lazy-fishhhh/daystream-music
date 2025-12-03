package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签到状态VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckInStatusVO {
    // 是否已经签到
    private boolean isCheckedIn;
    // 连续签到天数
    private int continuousDays;
    // 本月签到天数
    private int monthCheckInDays;
}
