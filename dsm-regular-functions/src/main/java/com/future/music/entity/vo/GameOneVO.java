package com.future.music.entity.vo;

import entity.pojo.music.MusicAudio;
import lombok.Data;

import java.util.List;

// 第一种游戏返回值
@Data
public class GameOneVO {
    private GameQuestionVO gameQuestionVO;
    private List<GameMusicVO> options;
}
