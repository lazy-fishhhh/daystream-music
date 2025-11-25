package com.future.music.domain.dto;

import lombok.Data;

/**
 * 单行歌词DTO（时间戳+文本）
 */
@Data
public class LyricLineDTO {
    /** 时间戳（毫秒） */
    private Long time;
    /** 歌词文本 */
    private String text;

    public LyricLineDTO(Long time, String text) {
        this.time = time;
        this.text = text;
    }
}