package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.future.music.entity.dto.StartGameDTO;
import com.future.music.entity.message.GameSession;
import com.future.music.entity.vo.GameOneVO;
import entity.pojo.util.Result;

public interface IGameService {

    /**
     * 开始游戏
     */
    Result<GameSession> startGame(StartGameDTO startGameDTO);

    /**
     * 获取下一题（单轮答题）
     */
    Result<GameOneVO> getNextQuestion(String sessionId);

    /**
     * 检查答案
     */
    Result<Object> checkAnswer(String sessionId, Long musicId, String answer);

    /**
     * 结束游戏（清理会话）
     */
    void endGame(String sessionId);
}
