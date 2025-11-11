package com.future.music.entity.pojo.social;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {
    private Long id;                 // 主键ID
    private Long userId;             // 创建者ID
    private String playlistName;     // 歌单名
    private String coverUrl;         // 封面URL
    private String description;      // 描述
    private Integer isPublic;        // 是否公开（0-私有，1-公开）
    private Integer musicCount;      // 歌曲数量
    private Long playCount;          // 播放量
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
}