package com.future.player.entity.vo;

import lombok.Data;

@Data
public class MusicianVO {
    /**
     * 音乐人id
     */
    private String id;
    /**
     * 艺名
     */
    private String stageName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 描述
     */
    private String introduction;
    /**
     * 代表作
     */
    private String representativeWork;
}
