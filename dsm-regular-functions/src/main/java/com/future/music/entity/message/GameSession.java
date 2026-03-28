package com.future.music.entity.message;

import com.future.music.entity.vo.GameMusicVO;
import io.swagger.models.auth.In;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class GameSession {
    /**
     * 游戏会话ID（唯一标识一场游戏）
     */
    private String sessionId;

    /**
     * 游戏类型id（1:听音猜歌 / 2:歌词接龙 / 3:猜作者）
     */
    private Integer gameTypeId;

    /**
     * 难度（简单:1 / 中等:2 / 困难:3）
     */
    private Integer difficulty;
    
    /**
     * 答题时间限制（简单:20 / 中等:15 / 困难:10）
     */
    private Integer timeLimit;
    
    /**
     * 总分（每题10分）
     */
    private Integer score = 0;
    
    /**
     * 当前连击数
     */
    private Integer combo = 0;
    
    /**
     * 最高连击数
     */
    private Integer maxCombo = 0;
    
    /**
     * 当前答题轮次（1-10）
     */
    private Integer currentRound = 0;

    /**
     * 已使用的歌曲ID（确保10题不重复）
     */
    private Set<Long> usedSongIds = new HashSet<>();

}