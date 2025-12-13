package com.future.music.entity.message;

import com.future.music.domain.vo.MusicTagVO;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 音乐播放上报的MQ消息体
 */
@Data
public class MusicPlayReportMqMessage {
    /** 用户ID */
    private Long userId;
    /** 音乐ID */
    private Long musicId;
    /** 是否计入有效播放（1=是，0=否） */
    private Integer listenCount;
    /** 歌曲标签列表（提前查询，避免消费者重复查库） */
    private List<MusicTagVO> tagList;
    /** 当前日期（用于周ID生成） */
    private LocalDate currentDate;
}