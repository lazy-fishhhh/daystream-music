package com.future.music.domain.vo;

import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectorVO extends SearchUserVO {
    private Integer fansCount;

    // 收藏的歌单数量
    private Long collectPlaylistCount;
}
