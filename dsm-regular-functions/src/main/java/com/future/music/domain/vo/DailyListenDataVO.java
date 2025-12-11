package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VO类用于返回每日的听歌数据，包括次数和时长
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyListenDataVO {
    private Integer count;       // 听歌次数
    private Long seconds;        // 听歌时长（秒）
}
