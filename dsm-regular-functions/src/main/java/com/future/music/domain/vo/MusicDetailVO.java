package com.future.music.domain.vo;

import entity.pojo.music.Music;
import entity.pojo.music.MusicAudio;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MusicDetailVO {
    private Long id;                    // 主键ID
    private String musicName;           // 歌曲名
    private Long albumId;               // 所属专辑ID
    private Long musicianId;            // 音乐人ID
    private String duration;            // 时长（秒）
    private String lyricUrl;            // 歌词URL
    private String coverUrl;            // 封面URL
    private Integer bpm;                // 节奏（BPM）
    private Integer isVip;              // 是否VIP专属（0-否，1-是）
    private BigDecimal price;           // 单价（音浪）

    private List<MusicAudio> audioList; // 音频列表
    private List<String> tags;          // 歌曲标签（需关联标签表）

    private Integer isLiked;             //是否点赞（0-未点赞，1-已点赞）
    private Integer isCollected;          //是否收藏
    private String albumName;           // 所属专辑名字
    private String musicianName;        // 作者名字（音乐人名称）

    public void copyFrom(Music music){
        this.id = music.getId();
        this.musicName = music.getMusicName();
        this.albumId = music.getAlbumId();
        this.musicianId = music.getMusicianId();
        this.duration = music.getDuration();
        this.lyricUrl = music.getLyricUrl();
        this.coverUrl = music.getCoverUrl();
        this.bpm = music.getBpm();
        this.isVip = music.getIsVip();
        this.price = music.getPrice();
    }
}
