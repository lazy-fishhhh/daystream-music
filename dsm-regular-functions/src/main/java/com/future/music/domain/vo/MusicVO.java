package com.future.music.domain.vo;

import com.future.music.domain.dto.LyricLineDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicVO {
    private Long id;                    // 主键ID
    private String musicName;           // 歌曲名
    private Long albumId;               // 所属专辑ID
    private Long musicianId;            // 音乐人ID
    private String coverUrl;            // 封面URL
    private Integer bpm;                // 节奏（BPM）
    private Integer isVip;              // 是否VIP专属（0-否，1-是）
    private Long likeCount;             // 点赞量
    private Long commentCount;          // 评论量

    private String duration;            // 时长
    private Integer isLiked;            // 是否点赞（0-未点赞，1-已点赞）
    private String albumName;           // 所属专辑名字
    private String musicianName;        // 作者名字（音乐人名称）
    private List<String> audioList;     // 音频类型列表
}
