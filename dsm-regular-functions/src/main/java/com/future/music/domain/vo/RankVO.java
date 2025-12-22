package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankVO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime updatedTime;

    private List<MusicVO> musicList = new ArrayList<>();
}
