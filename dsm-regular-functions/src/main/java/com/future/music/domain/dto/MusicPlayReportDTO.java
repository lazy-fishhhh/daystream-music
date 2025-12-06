package com.future.music.domain.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class MusicPlayReportDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;          // 用户ID（必填，未登录不计入）

    @NotNull(message = "歌曲ID不能为空")
    private Long musicId;         // 歌曲ID（必填）

    @NotNull(message = "有效播放时长不能为空")
    @Min(value = 0, message = "有效播放时长不能为负数")
    private Integer playedDuration;  // 本次上报的有效播放时长（秒，必填）

    @NotNull(message = "歌曲总时长不能为空")
    @Min(value = 1, message = "歌曲总时长必须大于0")
    private Integer totalDuration;   // 歌曲总时长（秒，必填）
}