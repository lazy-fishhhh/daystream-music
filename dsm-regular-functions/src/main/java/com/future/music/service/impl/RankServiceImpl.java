package com.future.music.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.dto.LyricLineDTO;
import com.future.music.domain.vo.*;
import com.future.music.mapper.MusicianMapper;
import com.future.music.mapper.PlaylistMapper;
import com.future.music.mapper.PlaylistMusicMapper;
import com.future.music.service.IMusicService;
import com.future.music.service.IRankService;
import com.future.music.util.LyricParseUtil;
import constants.MusicConstants;
import constants.RankConstants;
import constants.RedisCacheConstants;
import entity.pojo.music.*;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RankServiceImpl implements IRankService {

    @Resource
    private IMusicService musicService;
    @Resource
    private PlaylistMusicMapper playlistMusicMapper;
    @Resource
    private PlaylistMapper playlistMapper;
    @Resource
    private MusicianMapper musicianMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    // 进入榜单
    @Override
    public Result<RankHomeVO> getRankHome(){
        try {
            // 1. 构建缓存Key（按日期划分，与定时任务保持一致）
            LocalDate currentDate = LocalDate.now();
            String cacheKey = RankConstants.CACHE_KEY_RANK_HOME + currentDate;
            log.info("获取榜单首页数据，cacheKey: {}", cacheKey);

            // 2. 优先查询缓存
            RankHomeVO rankHomeVO = getRankHomeFromCache(cacheKey);
            if (rankHomeVO != null) {
                log.info("榜单首页缓存命中，直接返回数据，cacheKey: {}", cacheKey);
                return Result.ok(rankHomeVO);
            }

            log.warn("榜单首页缓存未命中，查询数据库并更新缓存，cacheKey: {}", cacheKey);
            // 3. 缓存未命中，查询数据库
            rankHomeVO = getRankHomeVO();

            // 4. 写入缓存（兜底更新，定时任务会主动更新，这里设置相同的过期时间）
            try {
                String jsonStr = JSON.toJSONString(rankHomeVO);
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        jsonStr,
                        RedisCacheConstants.CACHE_LONG_TTL,
                        TimeUnit.SECONDS
                );
                log.info("榜单首页数据写入缓存成功，cacheKey: {}", cacheKey);
            } catch (Exception e) {
                log.error("榜单首页数据写入缓存失败，cacheKey: {}", cacheKey, e);
                // 缓存写入失败不影响业务，仅记录日志
            }

            return Result.ok(rankHomeVO);
        } catch (Exception e) {
            log.error("获取榜单首页数据失败", e);
            return Result.fail("进入榜单失败：" + e.getMessage());
        }
    }
    /**
     * 从缓存中获取榜单首页数据
     *
     * @param cacheKey 缓存Key
     * @return 榜单首页VO
     */
    private RankHomeVO getRankHomeFromCache(String cacheKey) {
        try {
            String cacheJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheJson == null || cacheJson.isEmpty()) {
                return null;
            }
            // 反序列化为对象
            return JSON.parseObject(cacheJson, RankHomeVO.class);
        } catch (Exception e) {
            log.error("从缓存读取榜单首页数据失败，cacheKey: {}", cacheKey, e);
            // 缓存反序列化失败，返回null走数据库查询
            return null;
        }
    }
    /**
     * 聚合榜单首页数据（数据库查询逻辑）
     */
    public RankHomeVO getRankHomeVO() {
        Page<Music> musicPage = new Page<>(1, 6);

        // 查询热歌榜
        RankVO hotRank = buildRankVO(RankConstants.HOT_MUSIC_RANK_ID, musicPage);
        // 查询新歌榜
        RankVO newRank = buildRankVO(RankConstants.NEW_MUSIC_RANK_ID, musicPage);
        // 查询VIP热歌榜
        RankVO vipHotRank = buildRankVO(RankConstants.VIP_HOT_MUSIC_RANK_ID, musicPage);
        // 查询VIP新歌榜
        RankVO vipNewRank = buildRankVO(RankConstants.VIP_NEW_MUSIC_RANK_ID, musicPage);

        // 查询音乐人榜
        List<MusicianVO> musicianVoList = buildMusicianVOList();

        // 构建榜单首页VO
        Map<String, RankVO> rankVOMap = new HashMap<>();
        rankVOMap.put("hotRank", hotRank);
        rankVOMap.put("newRank", newRank);
        rankVOMap.put("vipHotRank", vipHotRank);
        rankVOMap.put("vipNewRank", vipNewRank);

        RankHomeVO rankHomeVO = new RankHomeVO();
        rankHomeVO.setRankVOMap(rankVOMap);
        rankHomeVO.setMusicianList(musicianVoList);

        return rankHomeVO;
    }
    /**
     * 构建单个榜单VO（抽取重复逻辑，提高代码复用性）
     *
     * @param playlistId 榜单歌单ID
     * @param musicPage  分页参数
     * @return 榜单VO
     */
    private RankVO buildRankVO(Long playlistId, Page<Music> musicPage) {
        RankVO rankVO = new RankVO();
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            log.warn("榜单歌单不存在，playlistId: {}", playlistId);
            return rankVO;
        }
        BeanUtils.copyProperties(playlist, rankVO);

        // 查询榜单中的音乐
        IPage<Music> musicIPage = playlistMusicMapper.selectMusicsByPlaylistId(musicPage, playlist.getId());
        for (Music music : musicIPage.getRecords()) {
            MusicVO musicVO = musicService.getMusicVO(music.getId());
            rankVO.getMusicList().add(musicVO);
        }
        return rankVO;
    }

    /**
     * 构建音乐人VO列表
     *
     * @return 音乐人VO列表
     */
    private List<MusicianVO> buildMusicianVOList() {
        List<MusicianVO> musicianVoList = new ArrayList<>();
        List<Musician> musicianList = musicianMapper.selectList(new LambdaQueryWrapper<Musician>()
                .orderByDesc(Musician::getTotalFans)
                .last("limit 10"));

        for (Musician musician : musicianList) {
            MusicianVO musicianVO = new MusicianVO();
            BeanUtils.copyProperties(musician, musicianVO);

            // 查询音乐人热门音乐
            List<String> hotMusics = musicService.listObjs(
                    new LambdaQueryWrapper<Music>()
                            .eq(Music::getMusicianId, musician.getId())
                            .select(Music::getMusicName)
                            .orderByDesc(Music::getPlayCount)
                            .last("limit 3"),
                    r -> (String) r
            );
            musicianVO.setHotMusics(hotMusics);
            musicianVoList.add(musicianVO);
        }
        return musicianVoList;
    }

    /**
     * 更新榜单缓存
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateRankHomeCache(){
        LocalDate currentDate = LocalDate.now();
        String cacheKey = RankConstants.CACHE_KEY_RANK_HOME + currentDate;
        String lockKey = RankConstants.LOCK_KEY_RANK_HOME;
        log.info("开始执行榜单缓存更新任务，cacheKey: {}, lockKey: {}", cacheKey, lockKey);

        try {
            // 查缓存
            String cacheJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheJson != null && !cacheJson.isEmpty()) {
                log.info("缓存已命中，无需更新，cacheKey: {}", cacheKey);
                return;
            }
            log.warn("缓存未命中，准备获取分布式锁，cacheKey: {}", cacheKey);

            // 获取分布式锁
            RLock lock = redissonClient.getLock(lockKey);
            boolean isLockAcquired = false;
            try {
                isLockAcquired = lock.tryLock(RedisCacheConstants.LOCK_WAIT_TIME, RedisCacheConstants.LOCK_EXPIRE_TTL, TimeUnit.SECONDS);
                if (!isLockAcquired) {
                    log.warn("获取分布式锁失败，其他线程正在重建缓存，cacheKey: {}", cacheKey);
                    return;
                }
                log.info("成功获取分布式锁，开始双重检查缓存");

                // 双重检查缓存
                cacheJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cacheJson != null && !cacheJson.isEmpty()) {
                    log.info("双重检查缓存命中，无需查询DB，cacheKey: {}", cacheKey);
                    return;
                }

                // 查DB
                log.info("开始查询DB聚合榜单数据，cacheKey: {}", cacheKey);
                long start = System.currentTimeMillis();
                RankHomeVO rankHomeVO = getRankHomeVO();
                long cost = System.currentTimeMillis() - start;
                log.info("DB查询完成，耗时{}ms，cacheKey: {}", cost, cacheKey);

                // 写入缓存
                String jsonStr = JSON.toJSONString(rankHomeVO);
                stringRedisTemplate.opsForValue().set(cacheKey, jsonStr, RedisCacheConstants.CACHE_LONG_TTL, TimeUnit.SECONDS);
                log.info("缓存写入成功，过期时间{}小时，cacheKey: {}", RedisCacheConstants.CACHE_LONG_TTL / 3600, cacheKey);

            } catch (Exception e) {
                log.error("获取锁后执行缓存重建失败，cacheKey: {}", cacheKey, e);
                throw e; // 若有全局异常处理器，可抛出；若无，可记录后返回
            } finally {
                // 释放锁
                if (isLockAcquired && lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                        log.info("成功释放分布式锁，lockKey: {}", lockKey);
                    } catch (Exception e) {
                        log.error("释放分布式锁失败，lockKey: {}", lockKey, e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("榜单缓存更新任务执行失败，cacheKey: {}", cacheKey, e);
        }
    }

    /**
     * 更新热歌榜
     * 每日凌晨1点0分0秒执行（cron表达式：秒 分 时 日 月 周 年（可选））
     */
    @Scheduled(cron = "0 52 23 * * ?")
    public void updateHotMusicRank() {
        String rankName = "热歌榜";
        Long rankId = RankConstants.HOT_MUSIC_RANK_ID;
        log.info("开始执行【{}】更新任务，rankId: {}", rankName, rankId);

        try {
            // 从数据库中根据播放量从高到低排序查询出前100首音乐的id
            List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                            .select(Music::getId) // 仅查询ID字段，减少数据量
                            .orderByDesc(Music::getPlayCount)
                            .last("limit 100"),
                    r -> ((Number) r).longValue()
            );
            log.info("【{}】查询到符合条件的音乐数量：{}首", rankName, musicIds.size());

            // 处理榜单的创建或更新
            handleRankCreateOrUpdate(rankId, rankName, "热歌榜：所有线上歌曲官方TOP排行榜，每日更新。", musicIds);

            log.info("【{}】更新任务执行完成，rankId: {}", rankName, rankId);
        } catch (Exception e) {
            log.error("【{}】更新任务执行失败，rankId: {}", rankName, rankId, e);
        }
    }

    /**
     * 更新新歌榜
     * 每日凌晨1点10分0秒执行（错开热歌榜更新时间，降低并发压力）
     */
    @Scheduled(cron = "0 54 23 * * ?")
    public void updateNewMusicRank() {
        String rankName = "新歌榜";
        Long rankId = RankConstants.NEW_MUSIC_RANK_ID;
        log.info("开始执行【{}】更新任务，rankId: {}", rankName, rankId);

        try {
            // 从数据库中查询按播放量从高到低排序一个月内发布的音乐id
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                            .select(Music::getId) // 仅查询ID字段，减少数据量
                            .ge(Music::getCreateTime, oneMonthAgo) // 发布时间 >= 一个月前
                            .orderByDesc(Music::getPlayCount) // 按播放量降序
                            .last("limit 100"), // 限制结果数量为100
                    r -> ((Number) r).longValue()
            );
            log.info("【{}】查询到近一个月发布的符合条件的音乐数量：{}首", rankName, musicIds.size());

            // 处理榜单的创建或更新
            handleRankCreateOrUpdate(rankId, rankName, "新歌榜：用户收听所有新歌（一月内最新发行） 官方TOP排行榜，每天更新。", musicIds);

            log.info("【{}】更新任务执行完成，rankId: {}", rankName, rankId);
        } catch (Exception e) {
            log.error("【{}】更新任务执行失败，rankId: {}", rankName, rankId, e);
        }
    }

    /**
     * 更新VIP热歌榜
     * 每日凌晨1点20分0秒执行
     */
    @Scheduled(cron = "0 56 23 * * ?")
    public void updateVipHotMusicRank() {
        String rankName = "VIP热歌榜";
        Long rankId = RankConstants.VIP_HOT_MUSIC_RANK_ID;
        log.info("开始执行【{}】更新任务，rankId: {}", rankName, rankId);

        try {
            // 从数据库中根据播放量从高到低排序查询出前50首vip音乐的id
            List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                            .select(Music::getId) // 仅查询ID字段，减少数据量
                            .eq(Music::getIsVip, 1) // 仅查询VIP音乐
                            .orderByDesc(Music::getPlayCount)
                            .last("limit 50"),
                    r -> ((Number) r).longValue()
            );
            log.info("【{}】查询到符合条件的VIP音乐数量：{}首", rankName, musicIds.size());

            // 处理榜单的创建或更新
            handleRankCreateOrUpdate(rankId, rankName, "VIP热歌榜：所有VIP线上歌曲官方TOP排行榜，每日更新。", musicIds);

            log.info("【{}】更新任务执行完成，rankId: {}", rankName, rankId);
        } catch (Exception e) {
            log.error("【{}】更新任务执行失败，rankId: {}", rankName, rankId, e);
        }
    }

    /**
     * 更新VIP新歌榜
     * 每日凌晨1点30分0秒执行
     */
    @Scheduled(cron = "0 58 23 * * ?")
    public void updateVipNewMusicRank() {
        String rankName = "VIP新歌榜";
        Long rankId = RankConstants.VIP_NEW_MUSIC_RANK_ID;
        log.info("开始执行【{}】更新任务，rankId: {}", rankName, rankId);

        try {
            // 从数据库中查询按播放量从高到低排序一个月内发布的VIP音乐id
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                            .select(Music::getId) // 仅查询ID字段，减少数据量
                            .eq(Music::getIsVip, 1) // 仅查询VIP音乐
                            .ge(Music::getCreateTime, oneMonthAgo) // 发布时间 >= 一个月前
                            .orderByDesc(Music::getPlayCount) // 按播放量降序
                            .last("limit 50"), // 限制结果数量为50
                    r -> ((Number) r).longValue()
            );
            log.info("【{}】查询到近一个月发布的符合条件的VIP音乐数量：{}首", rankName, musicIds.size());

            // 处理榜单的创建或更新
            handleRankCreateOrUpdate(rankId, rankName, "VIP新歌榜：VIP线上新歌（一个月内最新发行）官方TOP排行榜，每日更新。", musicIds);

            log.info("【{}】更新任务执行完成，rankId: {}", rankName, rankId);
        } catch (Exception e) {
            log.error("【{}】更新任务执行失败，rankId: {}", rankName, rankId, e);
        }
    }

    /**
     * 统一处理榜单的创建或更新逻辑（提取重复代码，减少冗余）
     * @param rankId 榜单ID
     * @param rankName 榜单名称
     * @param description 榜单描述
     * @param musicIds 音乐ID列表
     */
    private void handleRankCreateOrUpdate(Long rankId, String rankName, String description, List<Long> musicIds) {
        // 检查榜单是否存在
        if (playlistMapper.selectById(rankId) == null) {
            log.warn("【{}】不存在，开始创建新榜单，rankId: {}", rankName, rankId);
            // 创建新榜单
            Playlist playlist = new Playlist();
            playlist.setId(rankId);
            playlist.setName(rankName);
            playlist.setUserId(15L);
            playlist.setDescription(description);
            playlist.setIsPublic(1);
            playlist.setMusicCount((long) musicIds.size());
            playlist.setPlayCount(0L);
            playlist.setCreatedTime(LocalDateTime.now());
            playlist.setUpdatedTime(LocalDateTime.now());
            playlist.setCollectCount(0L);
            playlist.setCommentCount(0L);

            playlistMapper.insert(playlist);
            log.info("【{}】新榜单创建成功，rankId: {}", rankName, rankId);

            // 添加音乐到榜单
            addMusicToRank(rankId, musicIds);
            log.info("【{}】初始音乐添加完成，rankId: {}", rankName, rankId);
            return;
        }

        // 榜单已存在，清空原有音乐
        log.info("【{}】已存在，开始清空原有音乐，rankId: {}", rankName, rankId);
        playlistMusicMapper.delete(new LambdaQueryWrapper<PlaylistMusic>()
                .eq(PlaylistMusic::getPlaylistId, rankId));
        log.info("【{}】原有音乐清空完成，rankId: {}", rankName, rankId);

        // 添加新的音乐到榜单
        addMusicToRank(rankId, musicIds);
        log.info("【{}】新音乐添加完成，rankId: {}", rankName, rankId);
    }

    // 将音乐添加到榜单中
    private void addMusicToRank(Long rankId, List<Long> musicIds) {
        int sortOrder = 1;
        for (Long musicId : musicIds) {
            PlaylistMusic playlistMusic = new PlaylistMusic();
            playlistMusic.setPlaylistId(rankId);
            playlistMusic.setMusicId(musicId);
            playlistMusic.setSortOrder(sortOrder++);
            playlistMusic.setAddedTime(LocalDateTime.now());
            playlistMusicMapper.insert(playlistMusic);
        }
        // 更新榜单中的音乐数量
        Playlist playlist = playlistMapper.selectById(rankId);
        playlist.setMusicCount((long) musicIds.size());
        playlist.setUpdatedTime(LocalDateTime.now());
        playlistMapper.updateById(playlist);
    }

}
