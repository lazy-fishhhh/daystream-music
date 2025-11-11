package com.future.music.entity.pojo.music;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListenHistory {
    private Long id;                 // 主键ID
    private Long userId;             // 用户ID
    private Long musicId;            // 音乐ID
    private Integer listenCount;     // 累计播放次数
}