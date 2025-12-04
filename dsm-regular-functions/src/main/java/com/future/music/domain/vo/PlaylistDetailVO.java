package com.future.music.domain.vo;

import entity.pojo.util.PageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaylistDetailVO {
    private String name;                // 歌单名字
    private String cover;               // 封面
    private String description;         // 简介
    private Long musicCount;            // 音乐数
    private Long playCount;             // 播放数
    private LocalDateTime createdTime;  // 创建时间
    private Long commentCount;          // 评论数
    private Long collectionCount;       // 收藏数

    private PageResp<MusicVO> musicDetailVOList;  // 音乐
}
