package com.future.music.service.impl;

import com.alibaba.fastjson2.JSON;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.vo.MusicVO;
import com.future.music.mapper.MusicMapper;
import entity.pojo.util.Recommendation;
import entity.vo.MusicRecommendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import social.mapper.LikeRecord1Mapper;
import util.MusicHotScoreCalculator;
import util.UserBasedCF;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private static final String REDIS_HASH_PREFIX = "recommend:music:info:"; // Hash：歌曲详情（key=recommend:music:info:{songId}）
    private static final long REDIS_EXPIRE_DAYS = 2;                    // Redis缓存过期时间（7天）
    private static final String USER_TASK_PROGRESS_PREFIX = "user:task:progress:"; // 用户任务进度缓存前缀


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
     * 核心逻辑干了三个事：
     * 1. loadAllUserBehavior加载全量用户行为数据
     * 2. UCF类的recommend方法为每个用户生成推荐结果
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
                    // 生成用户推荐结果
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
            // 缓存结果可能不足20首，补足
            cacheResult = supplementRecommendToN(targetUser, cacheResult);
            log.info("【推荐接口】用户{}从Redis缓存获取推荐结果，数量：{}", targetUser, cacheResult.size());
            return cacheResult;
        }

        // 2. 缓存未命中，实时计算（降级方案）
        log.warn("【推荐接口】用户{}Redis缓存未命中，触发实时计算", targetUser);
        Map<Long, Set<Long>> userBehavior = loadAllUserBehavior();
        if (CollectionUtils.isEmpty(userBehavior)) {
            // 无用户行为数据，直接返回热门歌曲
            log.info("【推荐接口】用户{}无行为数据，返回热门歌曲", targetUser);
            return supplementRecommendToN(targetUser, Collections.emptyList());
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
     * 定时任务：每12小时刷新热门歌曲列表
     */
    @Scheduled(cron = "0 0 */12 * * ?")
    public void refreshHotMusicList() {
        String taskName = "热门歌曲列表刷新任务";
        log.info("【定时任务开始】{}，执行时间：{}", taskName, DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询全量歌曲（按播放量预过滤，只查TOP200减少计算量）
            List<MusicRecommendVO> allMusic = musicMapper.selectAllMusicWithStat();
            if (CollectionUtils.isEmpty(allMusic)) {
                log.warn("【定时任务警告】{}，无歌曲数据，跳过热门计算", taskName);
                return;
            }

            // 2. 计算每首歌曲的热门得分
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            redisTemplate.delete(REDIS_ZSET_HOT_MUSIC); // 先删除旧数据

            for (MusicRecommendVO music : allMusic) {
                double hotScore = MusicHotScoreCalculator.calculateHotScore(music);
                if (hotScore > 0) { // 得分>0才加入
                    zSetOps.add(REDIS_ZSET_HOT_MUSIC, music.getId().toString(), hotScore);
                    // 同步更新歌曲详情Hash
                    String hashKey = REDIS_HASH_PREFIX + music.getId();
                    HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
                    hashOps.put(hashKey, MUSIC_ID, music.getId().toString());
                    hashOps.put(hashKey, SONG_NAME, StringUtils.hasText(music.getMusicName()) ? music.getMusicName() : "未知歌曲");
                    hashOps.put(hashKey, SINGER, StringUtils.hasText(music.getMusicianName()) ? music.getMusicianName() : "未知歌手");
                    hashOps.put(hashKey, COVER_URL, StringUtils.hasText(music.getCoverUrl()) ? music.getCoverUrl() : "默认封面URL");
                    redisTemplate.expire(hashKey, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);
                }
            }

            // 3. 设置热门歌曲ZSet过期时间（7天）
            redisTemplate.expire(REDIS_ZSET_HOT_MUSIC, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("【定时任务成功】{}，耗时{}ms，共计算{}首歌曲的热门得分", taskName, costTime, allMusic.size());
        } catch (Exception e) {
            log.error("【定时任务失败】{}，执行异常！", taskName, e);
        }
    }

    /**
     * 冷启动：补足推荐结果到N首（协同过滤结果 + 热门歌曲）
     * @param userId 目标用户ID
     * @param recommendList 协同过滤推荐结果
     * @return 补足后的推荐列表（总数=N）
     */
    private List<Recommendation> supplementRecommendToN(Long userId, List<Recommendation> recommendList) {
        if (CollectionUtils.isEmpty(recommendList)) {
            recommendList = new ArrayList<>();
        }
        int needSupplement = N - recommendList.size();
        if (needSupplement <= 0) {
            return recommendList.subList(0, N); // 超过N首则截断
        }

        // 1. 从Redis获取热门歌曲（按得分降序）
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> hotMusicTuples = zSetOps.reverseRangeWithScores(REDIS_ZSET_HOT_MUSIC, 0, needSupplement - 1);
        if (CollectionUtils.isEmpty(hotMusicTuples)) {
            log.warn("【推荐补足】用户{}无足够热门歌曲补足，当前推荐数：{}", userId, recommendList.size());
            return recommendList;
        }

        // 2. 这里不过滤掉热门歌曲中用户已喜欢的歌曲，因为QQ音乐偶尔也会推荐喜欢过的歌曲
        //  如果想要过滤也很简单
        //  Set<Long> likeRecordSongs =  userBehavior.get(userId)
        //  if (likeRecordSongs.contains(redis中取出的热门歌曲id)) {
        //         continue;
        //      }
        Set<Long> existingRecommendSongIds = recommendList.stream()
                .map(Recommendation::getItemId)
                .collect(Collectors.toSet());

        // 3. 组装热门歌曲推荐结果
            List<Recommendation> supplementList = new ArrayList<>();
            for (ZSetOperations.TypedTuple<Object> tuple : hotMusicTuples) {
                if (supplementList.size() >= needSupplement) {
                    break;
                }
                String songIdStr = tuple.getValue().toString();
                Long songId;
                try {
                    songId = Long.valueOf(songIdStr);
                } catch (NumberFormatException e) {
                    log.warn("【推荐补足】歌曲ID格式错误：{}", songIdStr, e);
                    continue;
                }
                // 已在推荐列表中
                if (existingRecommendSongIds.contains(songId)) {
                    continue;
                }
                // 从Hash获取歌曲详情
                String hashKey = REDIS_HASH_PREFIX + songId;
                HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
                String songName = getHashValue(hashOps, hashKey, SONG_NAME, songId, userId);
                String singer = getHashValue(hashOps, hashKey, SINGER, songId, userId);
                String coverUrl = getHashValue(hashOps, hashKey, COVER_URL, songId, userId);
                if (songName == null || singer == null || coverUrl == null) {
                    continue;
                }
                // 封装热门歌曲推荐结果（得分=热门得分，标记为热门推荐）
                Recommendation rec = new Recommendation();
                rec.setItemId(songId);
                // 非协同过滤推荐得分，置为null
                rec.setScore(null);
                // 热门得分 = Redis ZSet中的score值
                rec.setHotScore(Optional.ofNullable(tuple.getScore()).orElse(0.0));
                rec.setSongName(songName);
                rec.setSinger(singer);
                rec.setCoverUrl(coverUrl);
                // 添加音频列表
                try {
                    List<entity.pojo.music.MusicAudio> audioList = musicMapper.selectAudioByMusicId(songId);
                    if (!CollectionUtils.isEmpty(audioList)) {
                        rec.setAudioList(audioList);
                    }
                } catch (Exception e) {
                    log.error("【推荐补足】查询歌曲音频信息失败，songId: {}", songId, e);
                }
                supplementList.add(rec);
            }

        // 4. 合并推荐结果（个性化在前，热门在后）
        recommendList.addAll(supplementList);
        log.info("【推荐补足】用户{}推荐结果补足完成，原数量：{}，补足后：{}", userId, recommendList.size() - supplementList.size(), recommendList.size());
        return recommendList;
    }



    /**
     * 加载全量用户行为数据（用户ID -> 喜欢的歌曲ID集合）

     * 干了两个事
     * 1. 批量查询用户id
     * 2. 批量查询用户点赞记录
     * --> 由此得到userBehavior
     */
    private Map<Long, Set<Long>> loadAllUserBehavior() {
        ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();

        // 1. 先从Redis缓存获取
        String cacheJson = (String) valueOps.get(REDIS_KEY_USER_BEHAVIOR);
        if (cacheJson != null && !cacheJson.isEmpty()) {
            try {
                // 反序列化JSON为Map<Long, Set<Long>>
                return JSON.parseObject(cacheJson, new com.alibaba.fastjson2.TypeReference<Map<Long, Set<Long>>>() {});
            } catch (Exception e) {
                log.error("【用户行为缓存】反序列化失败，缓存JSON：{}", cacheJson, e);
                // 反序列化失败，删除脏缓存
                redisTemplate.delete(REDIS_KEY_USER_BEHAVIOR);
            }
        }

        // 2. 缓存未命中/失效，查询DB
        Map<Long, Set<Long>> userBehavior = new HashMap<>();
        try {
            List<Long> userIds = userMapper.selectAllUserId();
            if (CollectionUtils.isEmpty(userIds)) {
                return userBehavior;
            }

            List<Map<String, Object>> likeRecordResults = likeRecord1Mapper.selectLikeSongsByUserIds(userIds);
            if (!CollectionUtils.isEmpty(likeRecordResults)) {
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
            log.info("【数据加载】从DB加载{}个用户的行为数据", userBehavior.size());

            // 3. 回写Redis缓存（设置过期时间）
            String jsonStr = JSON.toJSONString(userBehavior);
            valueOps.set(REDIS_KEY_USER_BEHAVIOR, jsonStr, REDIS_USER_BEHAVIOR_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("【用户行为缓存】已写入Redis，过期时间{}小时", REDIS_USER_BEHAVIOR_EXPIRE_HOURS);

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
        // 1. 生成纯兴趣相似推荐结果（无热门歌曲）
        List<Recommendation> recommendList = userBasedCF.recommend(userId, K, N);
        if (CollectionUtils.isEmpty(recommendList)) {
            log.info("【推荐生成】用户{}无协同过滤推荐结果，使用热门歌曲补足", userId);
        } else {
            // 组装歌曲详情 + 严格赋值：score=相似度，hotScore=null
            List<Long> songIds = recommendList.stream().map(Recommendation::getItemId).collect(Collectors.toList());
            Map<Long, MusicVO> musicVOMap = batchQueryMusicDetail(songIds);
            Map<Long, List<entity.pojo.music.MusicAudio>> audioMap = batchQueryMusicAudio(songIds);
            for (Recommendation recommendation : recommendList) {
                Long songId = recommendation.getItemId();
                MusicVO musicVO = musicVOMap.get(songId);
                if (musicVO != null) {
                    recommendation.setSongName(musicVO.getMusicName());
                    recommendation.setSinger(musicVO.getMusicianName());
                    recommendation.setCoverUrl(musicVO.getCoverUrl());
                }
                // 添加音频列表
                List<entity.pojo.music.MusicAudio> audioList = audioMap.get(songId);
                if (audioList != null) {
                    recommendation.setAudioList(audioList);
                }
                recommendation.setHotScore(null); // 兴趣相似歌曲hotScore置空
            }
        }
        // 2. 补足热门歌曲（此时recommendList是纯兴趣相似，补足后热门排在后面）
        return supplementRecommendToN(userId, recommendList);
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
     * 批量查询歌曲音频信息
     * @param songIds 歌曲ID列表
     * @return 歌曲ID -> 音频信息列表Map
     */
    private Map<Long, List<entity.pojo.music.MusicAudio>> batchQueryMusicAudio(List<Long> songIds) {
        Map<Long, List<entity.pojo.music.MusicAudio>> audioMap = new HashMap<>();
        if (CollectionUtils.isEmpty(songIds)) {
            return audioMap;
        }
        try {
            for (Long songId : songIds) {
                List<entity.pojo.music.MusicAudio> audioList = musicMapper.selectAudioByMusicId(songId);
                if (!CollectionUtils.isEmpty(audioList)) {
                    audioMap.put(songId, audioList);
                }
            }
        } catch (Exception e) {
            log.error("【批量查询】查询歌曲音频信息失败，歌曲ID列表：{}", songIds, e);
        }
        return audioMap;
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

                    // 3. 写入ZSet（歌曲ID+得分）
                    zSetOps.add(zsetKey, rec.getItemId().toString(), rec.getScore());

                    // 4. 写入Hash（强制更新字段，无论Hash Key是否存在）
                    String hashKey = REDIS_HASH_PREFIX + rec.getItemId();
                    // 校验核心字段，为空则填充默认值（避免写入null）
                    String songName = StringUtils.hasText(rec.getSongName()) ? rec.getSongName() : "未知歌曲";
                    String singer = StringUtils.hasText(rec.getSinger()) ? rec.getSinger() : "未知歌手";
                    String coverUrl = StringUtils.hasText(rec.getCoverUrl()) ? rec.getCoverUrl() : "未知url";

                    // 强制写入/更新字段（覆盖旧值）
                    hashOps.put(hashKey, MUSIC_ID, rec.getItemId().toString());
                    hashOps.put(hashKey, SONG_NAME, songName);
                    hashOps.put(hashKey, SINGER, singer);
                    hashOps.put(hashKey, COVER_URL, coverUrl);
                    // 设置Hash过期时间
                    redisTemplate.expire(hashKey, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);

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
                log.info("【Redis读取】用户{}的推荐ZSet缓存不存在，key:{}", userId, zsetKey);
                return resultList;
            }

            // 2. 从ZSet获取Top-N推荐结果（按得分降序，带得分）
            Set<ZSetOperations.TypedTuple<Object>> typedTuples = zSetOps.reverseRangeWithScores(zsetKey, 0, N - 1);
            if (CollectionUtils.isEmpty(typedTuples)) {
                log.info("【Redis读取】用户{}的推荐ZSet无数据，key:{}", userId, zsetKey);
                return resultList;
            }

            // 3. 组装推荐结果（关联Hash中的歌曲详情）
            for (ZSetOperations.TypedTuple<Object> tuple : typedTuples) {
                // 防御：Tuple或Value为空时跳过
                if (tuple == null || tuple.getValue() == null) {
                    log.warn("【Redis读取】用户{}的推荐ZSet存在空值Tuple，跳过", userId);
                    continue;
                }

                String songIdStr = tuple.getValue().toString();
                Long songId;
                // 防御：songId转换异常
                try {
                    songId = Long.valueOf(songIdStr);
                } catch (NumberFormatException e) {
                    log.warn("【Redis读取】用户{}的推荐歌曲ID格式错误，值：{}", userId, songIdStr, e);
                    continue;
                }

                Double score = tuple.getScore();
                // 防御：得分为空时默认0
                if (score == null) {
                    score = 0.0;
                    log.warn("【Redis读取】用户{}的歌曲{}推荐得分为空，默认0", userId, songId);
                }

                // 4. 从Hash读取歌曲详情（先检查Hash Key是否存在）
                String hashKey = REDIS_HASH_PREFIX + songId;
                if (!redisTemplate.hasKey(hashKey)) {
                    log.warn("【Redis读取】歌曲{}详情Hash不存在，key:{}，用户{}", songId, hashKey, userId);
                    continue;
                }

                // 5. 读取Hash字段（封装通用方法，避免重复判空）
                String songName = getHashValue(hashOps, hashKey, SONG_NAME, songId, userId);
                String singer = getHashValue(hashOps, hashKey, SINGER, songId, userId);
                String coverUrl = getHashValue(hashOps, hashKey, COVER_URL, songId, userId);

                // 6. 核心：任一关键字段缺失则跳过该条推荐（避免返回不完整数据）
                if (songName == null || singer == null || coverUrl == null) {
                    log.warn("【Redis读取】歌曲{}的关键详情字段缺失，用户{}", songId, userId);
                    continue;
                }

                // 7. 封装推荐结果
                Recommendation rec = new Recommendation();
                rec.setItemId(songId);
                rec.setScore(score);
                //协同过滤结果 → score保留，hotScore置空
                rec.setHotScore(null);
                rec.setSongName(songName);
                rec.setSinger(singer);
                rec.setCoverUrl(coverUrl);
                resultList.add(rec);
            }

            log.info("【Redis读取】用户{}推荐结果读取成功，数量：{}", userId, resultList.size());
        } catch (Exception e) {
            log.error("【Redis读取】用户{}推荐结果读取失败", userId, e);
        }
        return resultList;
    }

    /**
     * 通用方法：读取Hash字段值并做非空校验
     * @param hashOps Hash操作对象
     * @param hashKey Hash Key
     * @param field 字段名
     * @param songId 歌曲ID（日志用）
     * @param userId 用户ID（日志用）
     * @return 字段值（null表示缺失/异常）
     */
    private String getHashValue(HashOperations<String, String, Object> hashOps,
                                String hashKey,
                                String field,
                                Long songId,
                                Long userId) {
        try {
            Object value = hashOps.get(hashKey, field);
            if (value == null) {
                log.warn("【Redis读取】歌曲{}的Hash字段{}为空，hashKey:{}，用户{}",
                        songId, field, hashKey, userId);
                return null;
            }
            return value.toString();
        } catch (Exception e) {
            log.error("【Redis读取】歌曲{}的Hash字段{}读取异常，hashKey:{}，用户{}",
                    songId, field, hashKey, userId, e);
            return null;
        }
    }

    /**
     * 定时任务：每天凌晨0点清理过期的任务进度缓存
     * cron表达式：0 0 0 * * ? → 每天凌晨0点执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredTaskProgressCache() {
        String taskName = "过期任务进度缓存清理任务";
        log.info("【定时任务开始】{}，执行时间：{}", taskName, DATE_TIME_FORMATTER.format(LocalDateTime.now()));

        long startTime = System.currentTimeMillis();
        try {
            // 1. 扫描所有任务进度缓存键
            Set<String> keys = redisTemplate.keys(USER_TASK_PROGRESS_PREFIX + "*");
            if (CollectionUtils.isEmpty(keys)) {
                log.info("【定时任务完成】{}，没有需要清理的缓存键", taskName);
                return;
            }

            // 2. 清理过期的缓存键
            int deletedCount = 0;
            for (String key : keys) {
                // 检查键是否已过期（实际上Redis会自动清理过期键，但为了保险起见）
                if (redisTemplate.getExpire(key) == -2) {
                    redisTemplate.delete(key);
                    deletedCount++;
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("【定时任务完成】{}，清理完成，共扫描{}个缓存键，删除{}个过期键，耗时{}ms", 
                    taskName, keys.size(), deletedCount, (endTime - startTime));
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("【定时任务失败】{}，执行异常，耗时{}ms", 
                    taskName, (endTime - startTime), e);
        }
    }
}