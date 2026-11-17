package com.future.music.service.impl;

import com.alibaba.fastjson2.JSON;
import com.future.music.entity.dto.StartGameDTO;
import com.future.music.entity.message.GameSession;
import com.future.music.entity.vo.GameMusicVO;
import com.future.music.entity.vo.GameOneVO;
import com.future.music.entity.vo.GameQuestionVO;
import com.future.music.mapper.MusicAudioMapper;
import com.future.music.mapper.MusicMapper;
import com.future.music.service.IGameService;
import com.future.music.service.IMinIoService;
import constants.GameConstants;
import entity.pojo.music.Music;
import entity.pojo.music.MusicAudio;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Resource
    private MusicAudioMapper musicAudioMapper;
    @Resource
    private IMinIoService iMinIoService;

    public GameServiceImpl(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    @Override
    public Result<GameSession> startGame(StartGameDTO startGameDTO) {

        // 生成唯一会话ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 创建游戏会话
        GameSession session = new GameSession();
        session.setSessionId(sessionId);
        session.setDifficulty(startGameDTO.getDifficulty());
        session.setGameTypeId(startGameDTO.getGameTypeId());

        if(Objects.equals(startGameDTO.getDifficulty(), GameConstants.SIMPLICITY)){
            session.setTimeLimit(GameConstants.SIMPLICITY_TIME_LIMIT);
        }else if(Objects.equals(startGameDTO.getDifficulty(), GameConstants.MEDIUM)){
            session.setTimeLimit(GameConstants.MEDIUM_TIME_LIMIT);
        }else if(Objects.equals(startGameDTO.getDifficulty(), GameConstants.HARD)){
            session.setTimeLimit(GameConstants.HARD_TIME_LIMIT);
        }

        // 存入Redis
        String jsonStr = JSON.toJSONString(session);
        stringRedisTemplate.opsForValue().set(
                REDIS_KEY_GAME_SESSION + sessionId,
                jsonStr,
                GAME_SESSION_EXPIRE,
                TimeUnit.MINUTES
        );

        return Result.ok(session);
    }

    /**
     * 获取下一题（单轮答题）
     */
    public Result<GameOneVO> getNextQuestion(String sessionId) {
        String cacheJson = stringRedisTemplate.opsForValue().get(REDIS_KEY_GAME_SESSION + sessionId);
        if (Objects.isNull(cacheJson)) {
            return Result.fail("游戏会话不存在");
        }

        GameSession session = JSON.parseObject(cacheJson, GameSession.class);

        // 检查是否已完成10轮
        if (session.getCurrentRound() >= 10) {
            return Result.fail("游戏已完成");
        }

        // 题目：随机选择一首音乐并生成播放链接
        MusicAudio correctMusic = musicAudioMapper.selectRandomAudio(session.getUsedSongIds());
        String playUrl = iMinIoService.generatePresignedUrl(correctMusic.getAudioUrl(), 1800); // 30分钟有效
        if (playUrl == null) {
            return Result.fail("播放链接生成失败");
        }
        GameQuestionVO gameQuestionVO = new GameQuestionVO();
        gameQuestionVO.setMusicId(correctMusic.getMusicId());
        gameQuestionVO.setAudioUrl(playUrl);

        // 题目：获取音乐详情
        GameMusicVO gameMusicVO = new GameMusicVO();
        BeanUtils.copyProperties(musicMapper.selectById(gameQuestionVO.getMusicId()), gameMusicVO);

        // 选项：随机选择3首音乐
        List<GameMusicVO> options = musicMapper.selectRandomOptions(gameMusicVO.getId(), 3);

        // 添加正确答案并随机打乱选项
        options.add(gameMusicVO);
        java.util.Collections.shuffle(options);

        session.getUsedSongIds().add(correctMusic.getMusicId());

        GameOneVO gameOneVO = new GameOneVO();
        gameOneVO.setGameQuestionVO(gameQuestionVO);
        gameOneVO.setOptions(options);

        // 更新Redis
        session.setCurrentRound(session.getCurrentRound() + 1);
        String jsonStr = JSON.toJSONString(session);
        stringRedisTemplate.opsForValue().set(
                REDIS_KEY_GAME_SESSION + sessionId,
                jsonStr,
                GAME_SESSION_EXPIRE,
                TimeUnit.MINUTES
        );

        return Result.ok(gameOneVO);
    }

    /**
     * 检查答案
     */
    public Result<Object> checkAnswer(String sessionId, Long musicId, String answer) {
        String cacheJson = stringRedisTemplate.opsForValue().get(REDIS_KEY_GAME_SESSION + sessionId);
        if (Objects.isNull(cacheJson)) {
            return Result.fail("游戏会话不存在");
        }

        GameSession session = JSON.parseObject(cacheJson, GameSession.class);

        Music m = musicMapper.selectById(musicId);
        // 答对
        if(m.getMusicName().equals(answer)){
            // 更新分数
            session.setScore(session.getScore() + GameConstants.SCORE_PER_QUESTION);
            // 更新连击数
            session.setCombo(session.getCombo() + 1);
            // 更新最高连击数
            session.setMaxCombo(Math.max(session.getMaxCombo(), session.getCombo()));
        }else{
            session.setCombo(0);
        }

        if (session.getCurrentRound() >= 10) {
            return Result.ok(session);
        }

        // 更新Redis
        String jsonStr = JSON.toJSONString(session);
        stringRedisTemplate.opsForValue().set(
                REDIS_KEY_GAME_SESSION + sessionId,
                jsonStr,
                GAME_SESSION_EXPIRE,
                TimeUnit.MINUTES
        );

        return Result.ok(true);
    }

    /**
     * 结束游戏（清理会话）
     */
    public void endGame(String sessionId) {
        stringRedisTemplate.delete(REDIS_KEY_GAME_SESSION + sessionId);
    }
}
