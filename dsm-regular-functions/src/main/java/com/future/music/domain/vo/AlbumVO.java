package com.future.music.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlbumVO {
    private Long id;
    private String albumName;
    private String musicianName;
    private String coverUrl;
    private LocalDateTime releaseDate;
    private String introduction;
    private Integer status;
    private Long collectCount;
    private Long commentCount;
    private Long musicCount;

    private List<MusicVO> musicList;
}
