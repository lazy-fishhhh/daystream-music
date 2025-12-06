package com.future.player.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
