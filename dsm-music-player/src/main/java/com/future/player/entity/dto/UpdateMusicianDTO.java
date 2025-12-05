package com.future.player.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMusicianDTO {
    /**
     * 音乐人Id
     */
    @NotBlank(message = "音乐人Id不能为空")
    private String id;
    /**
     *音乐人姓名
     */
    private String name;
    /**
     * 音乐人简介
     */
    private String introduction;
    /**
     * 音乐人头像
     */
    private String avatar;
}
