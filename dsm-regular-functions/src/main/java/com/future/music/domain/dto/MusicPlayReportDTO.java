package com.future.music.domain.dto;

import lombok.Data;

//前端每 30 秒上报一次当前播放状态，播放结束 / 暂停时补充上报，避免漏统计
@Data
public class MusicPlayReportDTO {
    private Long userId;          // 用户ID（必填，未登录不计入）
    private Long musicId;         // 歌曲ID（必填）
    private Integer playedDuration;  // 本次上报的有效播放时长（秒，必填）
    private Integer totalDuration;   // 歌曲总时长（秒，必填）
}