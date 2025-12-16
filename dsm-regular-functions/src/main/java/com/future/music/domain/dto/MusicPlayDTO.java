package com.future.music.domain.dto;

import lombok.Data;

@Data
public class MusicPlayDTO {
    private String musicId; // 对应原参数musicId
    private Integer quality; // 对应原参数quality
}
