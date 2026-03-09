package com.future.music.domain.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RankHomeVO {
    private Map<String, RankVO> rankVOMap;
    private List<MusicianVO> musicianList;
}
