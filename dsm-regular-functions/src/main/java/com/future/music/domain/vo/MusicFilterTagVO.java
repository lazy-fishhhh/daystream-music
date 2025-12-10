package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 筛选标签返回VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicFilterTagVO {
    /**
     * 流派标签列表 (tagType=1)
     */
    private List<MusicTagVO> genreTags;

    /**
     * 心情标签列表 (tagType=2)
     */
    private List<MusicTagVO> moodTags;

    /**
     * 场景标签列表 (tagType=3)
     */
    private List<MusicTagVO> sceneTags;

    /**
     * 乐器标签列表 (tagType=4)
     */
    private List<MusicTagVO> instrumentTags;
}