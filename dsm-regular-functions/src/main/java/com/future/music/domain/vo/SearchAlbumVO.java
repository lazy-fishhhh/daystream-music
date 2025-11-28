package com.future.music.domain.vo;

import entity.dto.SearchDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchAlbumVO extends AlbumVO {
    private String musicianName;
    private String cover;
    private LocalDateTime releaseDate;
}
