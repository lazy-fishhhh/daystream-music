package com.future.music.domain.dto;

import lombok.Data;

@Data
public class MusicPlayReportDTO {
    private Long userId;
    private Long musicId;       // 音乐ID
    private Integer playedDuration; // 用户实际播放时长
}