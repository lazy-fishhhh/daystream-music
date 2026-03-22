package com.future.music.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.music.entity.dto.StartGameDTO;
import com.future.music.entity.message.GameSession;
import com.future.music.entity.vo.GameMusicVO;
import com.future.music.mapper.MusicMapper;
import com.future.music.service.IGameService;
import constants.GameConstants;
import entity.pojo.music.Music;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class GameServiceImpl implements IGameService {

    private final MusicMapper musicMapper;

    private static final String REDIS_KEY_GAME_SESSION = "music:game:session:";
    private static final long GAME_SESSION_EXPIRE = 30L; // 游戏会话有效期30分钟

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public GameServiceImpl(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    @Override
    public Result<Void> startGame(StartGameDTO startGameDTO) {

        // 生成唯一会话ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 创建游戏会话
        GameSession session = new GameSession();
        session.setSessionId(sessionId);
        session.setDifficulty(startGameDTO.getDifficulty());
        session.setGameTypeId(startGameDTO.getGameTypeId());

        // 存入Redis
        String jsonStr = JSON.toJSONString(session);
        stringRedisTemplate.opsForValue().set(
                REDIS_KEY_GAME_SESSION + sessionId,
                jsonStr,
                GAME_SESSION_EXPIRE,
                TimeUnit.MINUTES
        );

        return Result.ok();
    }

    /**
     * 获取下一题（单轮答题）
     */
    public Result<Void> getNextQuestion(String sessionId) {
        String cacheJson = stringRedisTemplate.opsForValue().get(REDIS_KEY_GAME_SESSION + sessionId);
        if (Objects.isNull(cacheJson)) {
            return Result.fail("游戏会话不存在");
        }

        GameSession session = JSON.parseObject(cacheJson, GameSession.class);

        // 检查是否已完成10轮
        if (session.getCurrentRound() >= 10) {
            return Result.fail("游戏已完成");
        }

        // 题目：随机选择一首音乐
        GameMusicVO correctMusic = musicMapper.selectRandomMusic(session.getUsedSongIds());
        session.getUsedSongIds().add(correctMusic.getMusicId());

        // 选项：随机选择3首音乐
        List<GameMusicVO> options = musicMapper.selectRandomOptions(correctMusic.getMusicId(), 3);

        // 添加正确答案并随机打乱选项
        options.add(correctMusic);
        java.util.Collections.shuffle(options);

        return Result.ok();
    }

    /**
     * 结束游戏（清理会话）
     */
    public void endGame(String sessionId) {
        stringRedisTemplate.delete(REDIS_KEY_GAME_SESSION + sessionId);
    }
}
