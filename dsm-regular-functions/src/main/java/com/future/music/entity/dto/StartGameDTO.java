package com.future.music.entity.dto;

import lombok.Data;

@Data
public class StartGameDTO {
    /**
     * 游戏类型id（1:听音猜歌 / 2:歌词接龙 / 3:猜作者）
     */
    private Integer gameTypeId;

    /**
     * 难度（简单:1 / 中等:2 / 困难:3）
     */
    private Integer difficulty;
}
