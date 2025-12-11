package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VO类用于返回音乐标签占比数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicTagRatioVO {
    private String tagName;         // 标签名称
    private Long count = 0L;            // 播放次数
    private Double ratio = 0.0;          // 占比（百分比）
}
