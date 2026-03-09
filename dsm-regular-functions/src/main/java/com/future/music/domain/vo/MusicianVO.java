package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicianVO {
    private Long id;                 // 主键ID
    private Long userId;             // 关联用户ID
    private String stageName;        // 艺名
    private String avatar;           // 音乐人头像
    private Integer totalFans;       // 粉丝总数

    private List<String> hotMusics;  // 热门音乐
}
