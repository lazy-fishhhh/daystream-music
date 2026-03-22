package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.future.music.entity.dto.StartGameDTO;
import com.future.music.entity.message.GameSession;
import entity.pojo.util.Result;

public interface IGameService {

    /**
     * 开始游戏
     */
    Result<Void> startGame(StartGameDTO startGameDTO);

    /**
     * 结束游戏（清理会话）
     */
    void endGame(String sessionId);
}
