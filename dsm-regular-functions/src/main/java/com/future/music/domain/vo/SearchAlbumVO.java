package com.future.music.domain.vo;

import entity.vo.MusicAlbumVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchAlbumVO extends MusicAlbumVO {
    private String musicianName;
    private String cover;
    private LocalDateTime releaseDate;
}
