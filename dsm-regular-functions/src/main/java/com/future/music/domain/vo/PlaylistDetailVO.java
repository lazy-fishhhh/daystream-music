package com.future.music.domain.vo;

import entity.pojo.util.PageResp;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlaylistDetailVO {
    private String name;
    private String cover;
    private String description;
    private Long musicCount;
    private Long playCount;
    private LocalDateTime createdTime;
    private Long commentCount;
    private Long collectionCount;

    private PageResp<MusicDetailVO> musicDetailVOList;
}
