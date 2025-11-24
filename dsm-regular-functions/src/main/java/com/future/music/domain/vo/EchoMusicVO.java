package com.future.music.domain.vo;

import entity.pojo.music.MusicAudio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EchoMusicVO {
    private Long id;                    // 主键ID
    private String musicName;           // 歌曲名
    private String coverUrl;            // 封面URL
    private Integer bpm;                // 节奏（BPM）
    private String licenseType;         // 版权类型
    private Integer isVip;              // 是否VIP专属（0-否，1-是）
    private BigDecimal price;           // 单价（音浪）

    private String lyricUrl;            // 歌词URL
    private List<MusicAudio> audioList; // 音频列表
    private List<String> tags;          // 歌曲标签（需关联标签表）
    private String albumName;           // 所属专辑名字
    private List<AlbumVO> albumList;    // 专辑列表
}
