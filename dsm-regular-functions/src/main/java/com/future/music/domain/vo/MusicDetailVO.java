package com.future.music.domain.vo;

import com.future.music.domain.dto.LyricLineDTO;
import entity.pojo.music.Music;
import entity.pojo.music.MusicAudio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicDetailVO extends MusicVO {
    private List<String> tags;          // 歌曲标签（需关联标签表）
    private String lyricType;           // 歌词文件类型
    private String lyricUrl;            // 歌词文件地址
    private List<LyricLineDTO> syncLyric; // LRC/KRC同步歌词（带时间戳）
    private String rawLyricText; // TXT纯文本歌词（无时间戳，保留换行）
}
