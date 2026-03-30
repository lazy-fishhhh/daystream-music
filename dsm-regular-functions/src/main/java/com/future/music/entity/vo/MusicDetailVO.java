package com.future.music.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicDetailVO {
    private Long id;                    // 主键ID
    private String musicName;           // 歌曲名

    private String coverUrl;            // 封面URL
    private Integer bpm;                // 节奏（BPM）
    private Integer isVip;              // 是否VIP专属（0-否，1-是）
    private Long likeCount;             // 点赞量
    private Long commentCount;          // 评论量

    private String duration;            // 时长
    private String albumName;           // 所属专辑名字
    private String musicianName;        // 作者名字（音乐人名称）
    private String createTime;
    private Integer status;
    private List<String> tags;          // 歌曲标签
    private String tagName;             // 临时字段，用于接收XML查询返回的标签字符串
    private List<AudioInfo> audioList;  // 音频列表

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AudioInfo {
        private Integer qualityType;     // 音质类型（1-标准，2-高清，3-无损）
        private String audioUrl;         // 音频URL
        private Long fileSize;           // 文件大小
    }
}
