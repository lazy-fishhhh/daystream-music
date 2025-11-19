package com.future.music.domain.vo;

import lombok.Data;

@Data
public class MusicPlayVO {
    private String audioUrl;    // 音频文件URL
    private String musicName;   // 歌曲名
    private String musicianName;// 音乐人名称
    private String coverUrl;    //封面URL
    private String duration;    // 时长
    private Integer isVip;      // 是否VIP专属（0-否，1-是）
}
