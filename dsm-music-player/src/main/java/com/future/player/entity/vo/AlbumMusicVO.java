package com.future.player.entity.vo;

import entity.pojo.music.MusicAudio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 专辑中的音乐VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlbumMusicVO {
    private Long id;                 // 音乐ID
    private String musicName;        // 歌曲名
    private Integer duration;        // 时长
    private String coverUrl;         // 封面URL
    private Integer isVip;           // 是否VIP专属（0-否，1-是）
    private BigDecimal price;        // 单价（音浪）
    private Long playCount;          // 播放量
    private Long likeCount;          // 点赞量
    private LocalDateTime createTime;// 创建时间
    private List<MusicAudio> audioList; // 音频列表
}
