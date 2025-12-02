package com.future.music.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.vo.MusicVO;
import com.future.music.mapper.MusicMapper;
import entity.pojo.util.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import social.mapper.LikeRecord1Mapper;
import util.UserBasedCF;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.future.music.constants.MusicVoConstants.*;

/**
 * 基于用户协同过滤的推荐定时任务管理类
 * 功能：
 * 1. 定时刷新全量用户的推荐结果（Redis ZSet + Hash 缓存）
 * 2. 提供实时推荐查询接口
 * 技术：Spring @Scheduled + Redis（ZSet存推荐排序，Hash存歌曲详情）
 * 后续升级：接入XXL-Job实现分布式定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskManager {

    // 时间格式化器
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Redis Key 前缀
    private static final String REDIS_ZSET_PREFIX = "recommend:user:";   // ZSet：用户推荐列表（key=recommend:user:{userId}）
    private static final String REDIS_HASH_PREFIX = "music:info:";      // Hash：歌曲详情（key=music:info:{songId}）
    private static final long REDIS_EXPIRE_DAYS = 7;                    // Redis缓存过期时间（7天）


    // 依赖注入
    private final UserMapper userMapper;
    private final LikeRecord1Mapper likeRecord1Mapper;
    private final MusicMapper musicMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 推荐算法超参数
    private static final int K = 30;  // 相似用户数（参考QQ音乐）
    private static final int N = 20;  // 推荐歌曲数

    /**
     * 定时任务：每天凌晨2点刷新全量用户的推荐结果
     * cron表达式：0 0 2 * * ? → 每天凌晨2点执行
     * 核心逻辑：
     * 1. 加载全量用户行为数据
     * 2. 为每个用户生成推荐结果
     * 3. 写入Redis缓存（ZSet存歌曲ID+得分，Hash存歌曲详情）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshAllUserRecommend() {
        String taskName = "全量用户推荐结果刷新任务";
        log.info("【定时任务开始】{}，执行时间：{}", taskName, DATE_TIME_FORMATTER.format(LocalDateTime.now()));

        long startTime = System.currentTimeMillis();
        try {
            // 1. 加载全量用户行为数据（用户ID -> 喜欢的歌曲ID集合）
            Map<Long, Set<Long>> userBehavior = loadAllUserBehavior();
            if (CollectionUtils.isEmpty(userBehavior)) {
                log.warn("【定时任务警告】{}，无用户行为数据，跳过推荐计算", taskName);
                return;
            }

            // 2. 初始化推荐算法
            UserBasedCF userBasedCF = new UserBasedCF(userBehavior);

            // 3. 为每个用户生成推荐结果并写入Redis
            int successCount = 0;
            List<Long> userIds = userMapper.selectAllUserId();
            for (Long userId : userIds) {
                try {
                    List<Recommendation> recommendList = generateUserRecommend(userBasedCF, userId);
                    if (!CollectionUtils.isEmpty(recommendList)) {
                        // 将推荐结果写入Redis（ZSet + Hash）
                        writeRecommendToRedis(userId, recommendList);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("【定时任务异常】生成用户{}推荐结果失败", userId, e);
                }
            }

            // 4. 任务执行结果日志
            long costTime = System.currentTimeMillis() - startTime;
            log.info("【定时任务成功】{}，耗时{}ms，成功生成{}个用户的推荐结果（总用户数：{}）",
                    taskName, costTime, successCount, userIds.size());

        } catch (Exception e) {
            log.error("【定时任务失败】{}，执行异常！", taskName, e);
        }
    }

    /**
     * 实时推荐接口：为指定用户获取推荐结果
     * 优先从Redis缓存获取，缓存未命中则实时计算并回写缓存
     * @return 推荐结果列表（含歌曲详情）
     */
    public List<Recommendation> getRecommend() {
        Long targetUser = SecurityUtil.getCurrentUserId();
        // 1. 优先从Redis获取推荐结果
        List<Recommendation> cacheResult = readRecommendFromRedis(targetUser);
        if (!CollectionUtils.isEmpty(cacheResult)) {
            log.info("【推荐接口】用户{}从Redis缓存获取推荐结果，数量：{}", targetUser, cacheResult.size());
            return cacheResult;
        }

        // 2. 缓存未命中，实时计算（降级方案）
        log.warn("【推荐接口】用户{}Redis缓存未命中，触发实时计算", targetUser);
        Map<Long, Set<Long>> userBehavior = loadAllUserBehavior();
        if (CollectionUtils.isEmpty(userBehavior)) {
            return Collections.emptyList();
        }
        UserBasedCF userBasedCF = new UserBasedCF(userBehavior);
        List<Recommendation> recommendList = generateUserRecommend(userBasedCF, targetUser);

        // 3. 实时计算结果回写Redis
        if (!CollectionUtils.isEmpty(recommendList)) {
            writeRecommendToRedis(targetUser, recommendList);
        }
        return recommendList;
    }

    /**
     * 加载全量用户行为数据（用户ID -> 喜欢的歌曲ID集合）
     * 优化点：批量查询代替循环单查
     */
    private Map<Long, Set<Long>> loadAllUserBehavior() {
        Map<Long, Set<Long>> userBehavior = new HashMap<>();
        try {
            // 1. 批量获取全量用户ID
            List<Long> userIds = userMapper.selectAllUserId();
            if (CollectionUtils.isEmpty(userIds)) {
                return userBehavior;
            }

            // 2. 批量查询用户点赞记录
            List<Map<String, Object>> likeRecordResults = likeRecord1Mapper.selectLikeSongsByUserIds(userIds);
            if (!CollectionUtils.isEmpty(likeRecordResults)) {
                // 将查询结果转换为Map<Long, Set<Long>>格式并添加到userBehavior映射中
                for (Map<String, Object> result : likeRecordResults) {
                    Long userId = ((Number) result.get("key")).longValue();
                    String likeSongsStr = (String) result.get("value");
                    if (likeSongsStr != null) {
                        Set<Long> likeSongs = Arrays.stream(likeSongsStr.split(","))
                                .map(Long::parseLong)
                                .collect(Collectors.toSet());
                        userBehavior.put(userId, likeSongs);
                    }
                }
            }
            
            log.info("【数据加载】成功加载{}个用户的行为数据", userBehavior.size());
        } catch (Exception e) {
            log.error("【数据加载】加载用户行为数据失败", e);
        }
        return userBehavior;
    }

    /**
     * 为单个用户生成推荐结果（含歌曲详情组装）
     * @param userBasedCF 推荐算法实例
     * @param userId      目标用户ID
     * @return 含歌曲详情的推荐结果
     */
    private List<Recommendation> generateUserRecommend(UserBasedCF userBasedCF, Long userId) {
        // 1. 调用算法生成推荐结果（仅含歌曲ID和得分）
        List<Recommendation> recommendList = userBasedCF.recommend(userId, K, N);
        if (CollectionUtils.isEmpty(recommendList)) {
            log.info("【推荐生成】用户{}无推荐结果", userId);
            return Collections.emptyList();
        }

        // 2. 组装歌曲详情 (这里数据库是批量查询，优化性能)
        List<Long> songIds = recommendList.stream()
                .map(Recommendation::getItemId)
                .collect(Collectors.toList());
        Map<Long, MusicVO> musicVOMap = batchQueryMusicDetail(songIds);

        // 3. 填充歌曲详情到推荐结果
        for (Recommendation recommendation : recommendList) {
            Long songId = recommendation.getItemId();
            MusicVO musicVO = musicVOMap.get(songId);
            if (musicVO != null) {
                // 拷贝歌曲详情到推荐结果
                BeanUtil.copyProperties(musicVO, recommendation);
            } else {
                log.warn("【推荐生成】歌曲{}未查询到详情，用户{}", songId, userId);
            }
        }

        log.info("【推荐生成】用户{}生成{}条推荐结果", userId, recommendList.size());
        return recommendList;
    }

    /**
     * 批量查询歌曲详情（优化循环单查问题）
     * @param songIds 歌曲ID列表
     * @return 歌曲ID -> 歌曲详情Map
     */
    private Map<Long, MusicVO> batchQueryMusicDetail(List<Long> songIds) {
        Map<Long, MusicVO> musicVOMap = new HashMap<>();
        if (CollectionUtils.isEmpty(songIds)) {
            return musicVOMap;
        }
        try {
            // 批量查询
            List<MusicVO> musicVOList = musicMapper.selectMusicDetailByIds(songIds);
            for (MusicVO vo : musicVOList) {
                musicVOMap.put(vo.getId(), vo);
            }
        } catch (Exception e) {
            log.error("【批量查询】查询歌曲详情失败，歌曲ID列表：{}", songIds, e);
        }
        return musicVOMap;
    }

    /**
     * 将推荐结果写入Redis（ZSet + Hash）
     * @param userId        目标用户ID
     * @param recommendList 推荐结果列表
     */
    private void writeRecommendToRedis(Long userId, List<Recommendation> recommendList) {
        String zsetKey = REDIS_ZSET_PREFIX + userId;
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        try {
            // 1. 先删除旧的推荐结果（避免数据残留）
            redisTemplate.delete(zsetKey);

            // 2. 批量写入ZSet（key=用户ID，value=歌曲ID，score=推荐得分）
            for (Recommendation rec : recommendList) {
                // ZSet添加：歌曲ID作为value，推荐得分作为score（降序排列）
                zSetOps.add(zsetKey, rec.getItemId().toString(), rec.getScore());

                // 3. 写入Hash存储歌曲详情（按需初始化，避免重复写入）
                String hashKey = REDIS_HASH_PREFIX + rec.getItemId();
                if (!redisTemplate.hasKey(hashKey)) {
                    hashOps.put(hashKey, MUSIC_ID, rec.getItemId().toString());
                    hashOps.put(hashKey, SONG_NAME, rec.getSongName());
                    hashOps.put(hashKey, SINGER, rec.getSinger());
                    hashOps.put(hashKey, COVER_URL, rec.getCoverUrl());
                    // 设置Hash过期时间
                    redisTemplate.expire(hashKey, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);
                }
            }

            // 4. 设置ZSet过期时间（7天）
            redisTemplate.expire(zsetKey, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("【Redis写入】用户{}推荐结果写入成功，数量：{}", userId, recommendList.size());
        } catch (Exception e) {
            log.error("【Redis写入】用户{}推荐结果写入失败", userId, e);
        }
    }

    /**
     * 从Redis读取推荐结果（ZSet + Hash）
     * @param userId 目标用户ID
     * @return 含歌曲详情的推荐结果列表
     */
    private List<Recommendation> readRecommendFromRedis(Long userId) {
        String zsetKey = REDIS_ZSET_PREFIX + userId;
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        List<Recommendation> resultList = new ArrayList<>();
        try {
            // 1. 检查ZSet是否存在
            if (!redisTemplate.hasKey(zsetKey)) {
                return resultList;
            }

            // 2. 从ZSet获取Top-N推荐结果（按得分降序，带得分）
            Set<ZSetOperations.TypedTuple<Object>> typedTuples = zSetOps.reverseRangeWithScores(zsetKey, 0, N - 1);
            if (CollectionUtils.isEmpty(typedTuples)) {
                return resultList;
            }

            // 3. 组装推荐结果（关联Hash中的歌曲详情）
            for (ZSetOperations.TypedTuple<Object> tuple : typedTuples) {
                String songIdStr = tuple.getValue().toString();
                Long songId = Long.valueOf(songIdStr);
                Double score = tuple.getScore();

                // 4. 从Hash读取歌曲详情
                String hashKey = REDIS_HASH_PREFIX + songId;
                if (!redisTemplate.hasKey(hashKey)) {
                    log.warn("【Redis读取】歌曲{}详情不存在，用户{}", songId, userId);
                    continue;
                }

                // 5. 封装推荐结果
                Recommendation rec = new Recommendation();
                rec.setItemId(songId);
                rec.setScore(score);
                rec.setSongName(hashOps.get(hashKey, SONG_NAME).toString());
                rec.setSinger(hashOps.get(hashKey, SINGER).toString());
                rec.setCoverUrl(hashOps.get(hashKey, COVER_URL).toString());
                resultList.add(rec);
            }
            log.info("【Redis读取】用户{}推荐结果读取成功，数量：{}", userId, resultList.size());
        } catch (Exception e) {
            log.error("【Redis读取】用户{}推荐结果读取失败", userId, e);
        }
        return resultList;
    }
}