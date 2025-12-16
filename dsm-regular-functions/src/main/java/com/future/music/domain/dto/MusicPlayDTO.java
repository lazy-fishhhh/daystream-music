package com.future.music.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class MusicPlayDTO {
    @NotNull(message = "用户ID不能为空")
    private String musicId; // 对应原参数musicId
    @NotNull(message = "音质等级不能为空")
    private Integer quality; // 对应原参数quality
}
