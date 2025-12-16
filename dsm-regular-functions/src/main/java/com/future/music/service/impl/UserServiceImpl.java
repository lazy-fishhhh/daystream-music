package com.future.music.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.future.login.domain.LoginUserDetails;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.message.SignInMessage;
import com.future.music.domain.vo.*;
import com.future.music.mapper.*;
import com.future.music.service.IMinIoService;
import com.future.music.service.IMusicService;
import com.future.music.service.IMusicianService;
import com.future.music.service.IPlaylistService;
import com.future.music.service.IUserService;
import constants.RedisCacheConstants;
import constants.SystemConstants;
import entity.dto.PlayListDTO;
import entity.dto.SearchDTO;
import entity.pojo.info.User;
import entity.pojo.music.Playlist;
import entity.pojo.music.PlaylistTagRelation;
import entity.pojo.search.HotSearch;
import entity.pojo.search.SearchHistory;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.models.auth.In;
import jakarta.annotation.Resource;

import java.sql.SQLException;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.future.music.code.PlaylistErrorCode.PLAYLIST_NAME_EXIST;
import static com.future.music.constants.SignConstants.*;
import static com.future.music.constants.SignConstants.SIGN_IN_DAYS_5_REWORD;
import static com.future.music.util.constants.CHECK_IN_KEY;
import static constants.RabbitConstants.ROUTING_KEY_SIGN_IN_SUCCESS;
import static constants.RabbitConstants.SIGN_IN_SUCCESS_TOPIC_EXCHANGE;


@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl  implements IUserService {

    @Resource
    private IMusicService musicService;

    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    @Resource
    private IMusicianService musicianService;

    @Resource
    private IPlaylistService playlistService;

    @Resource
    private HotSearchMapper hotSearchMapper;

    @Resource
    private MusicTagMapper musicTagMapper;

    @Resource
    private MusicianMapper musicianMapper;

    @Resource
    private MusicMapper musicMapper;

    @Resource
    private RedissonClient redissonClient;




    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IMinIoService minIoService;

    //构造方法注入
    private final PlaylistMapper playListMapper;
    private final PlaylistTagRelationMapper playlistTagRelationMapper;
    public final RabbitTemplate rabbitTemplate;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    @Override
    public Result<PlaylistVO> createPlayList(PlayListDTO playListDTO) {
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getName, playListDTO.getName());
        Playlist playlist = playListMapper.selectOne(lqw);
        if (playlist != null){
            throw new BusinessException(PLAYLIST_NAME_EXIST.getCode(),"歌单名称已存在,不允许重复");
        }

        try {
            LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long userId = loginUser.getUser().getId();
            
            // 处理封面文件上传
            String coverUrl = null;
            if (playListDTO.getCoverFile() != null && !playListDTO.getCoverFile().isEmpty()) {
                coverUrl = minIoService.uploadFile(playListDTO.getCoverFile());
                if (coverUrl == null) {
                    throw new BusinessException("10002", "封面文件上传失败");
                }
            }
            
            Playlist playList = new Playlist();
            playList.setUserId(userId);
            playList.setName(playListDTO.getName());
            playList.setCover(coverUrl);
            playList.setDescription(playListDTO.getDescription());
            playList.setIsPublic(playListDTO.getIsPublic());
            playList.setCreatedTime(LocalDateTime.now());
            playList.setUpdatedTime(LocalDateTime.now());
            playListMapper.insert(playList);

            // 处理多个标签
            if (playListDTO.getTagIds() != null && !playListDTO.getTagIds().isEmpty()) {
                List<PlaylistTagRelation> tagRelations = new ArrayList<>();
                for (String tagId : playListDTO.getTagIds()) {
                    PlaylistTagRelation playListTagRelation = new PlaylistTagRelation();
                    playListTagRelation.setPlaylistId(playList.getId());
                    playListTagRelation.setTagId(Long.valueOf(tagId));
                    tagRelations.add(playListTagRelation);
                }
                // 使用批量插入
                playlistTagRelationMapper.insertBatch(tagRelations);
            }

            // 返回结果
            PlaylistVO playlistVO = new PlaylistVO();
            playlistVO.setId(playList.getId());
            playlistVO.setIdStr(String.valueOf(playList.getId()));
            playlistVO.setName(playList.getName());
            playlistVO.setCover(playList.getCover());
            playlistVO.setDescription(playList.getDescription());
            playlistVO.setTagIds(playListDTO.getTagIds());

            return Result.ok(playlistVO);
        }catch (DataIntegrityViolationException e) {
            throw new BusinessException("标签异常10001",  "请检查标签是否存在");
        } catch (Exception e) {
            throw new BusinessException("业务异常100001", "业务异常" + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void updatePlayList(PlayListDTO dto) {
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getId,dto.getId());
        Playlist playlist1 = playListMapper.selectOne(lqw);
        if (playlist1 == null){
            throw new BusinessException("10002","歌单不存在");
        }
        if (Objects.equals(playlist1.getName(), "我的喜欢") || !Objects.equals(dto.getName(), "我的喜欢")){
            throw new BusinessException("10001","此歌单名称不允许修改");
        }
        if (Objects.equals(playlist1.getDescription(), "我的喜欢") || !Objects.equals(dto.getDescription(), "我的喜欢")){
            throw new BusinessException("10001","此歌单描述不允许修改");
        }


        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = loginUser.getUser().getId();
        Long playListUserId = playlist1.getUserId();

        if (!playListUserId.equals(userId)) {
            throw new BusinessException("权限不足10003","您没有权限修改此歌单");
        }


        try {
            // 处理封面文件上传
            String coverUrl = playlist1.getCover(); // 默认使用原有封面
            if (dto.getCoverFile() != null && !dto.getCoverFile().isEmpty()) {
                coverUrl = minIoService.uploadFile(dto.getCoverFile());
                if (coverUrl == null) {
                    throw new BusinessException("10002", "封面文件上传失败");
                }
            }
            
            // 创建 PlayList 对象并设置 ID 属性
            Playlist playlist = new Playlist();
            //updateById是通过id来更新的，所以必须设置id
            playlist.setId(dto.getId());
            playlist.setName(dto.getName());
            playlist.setCover(coverUrl);
            playlist.setDescription(dto.getDescription());
            playlist.setIsPublic(dto.getIsPublic());
            playlist.setUpdatedTime(LocalDateTime.now());

            // 使用 MyBatis-Plus 的 updateById 方法
            playListMapper.updateById(playlist);

            // 先删除该歌单当前所有的标签关联
            QueryWrapper<PlaylistTagRelation> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("playlist_id", dto.getId());
            playlistTagRelationMapper.delete(queryWrapper);

            // 为新的标签列表创建新的标签关联
            if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
                List<PlaylistTagRelation> tagRelations = new ArrayList<>();
                for (String tagId : dto.getTagIds()) {
                    PlaylistTagRelation playListTagRelation = new PlaylistTagRelation();
                    playListTagRelation.setPlaylistId(dto.getId());
                    playListTagRelation.setTagId(Long.valueOf(tagId));
                    tagRelations.add(playListTagRelation);
                }
                // 批量插入
                playlistTagRelationMapper.insertBatch(tagRelations);
            }

            String cacheKeyPattern = RedisCacheConstants.CACHE_KEY_PLAYLIST_DETAIL + dto.getId() + ":*";
            stringRedisTemplate.delete(cacheKeyPattern);
        }catch (DataIntegrityViolationException e) {
            throw new BusinessException("标签异常10001",  "请检查标签是否存在");
        } catch (Exception e) {
            throw new BusinessException("业务异常100001", "业务异常" + e.getMessage());
        }
    }

    @Override
    public PageResp<Playlist> getPlayList(PageReq pageReq) {
        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId  = loginUser.getUser().getId();
        // 泛型 <PlayList> 明确指定了该查询条件是针对 PlayList 实体类的
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getUserId, userId);

        // 1. 构建 MyBatis-Plus 的 Page 对象
        Page<Playlist> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
        // 2. 执行分页查询
        IPage<Playlist> playListPage = playListMapper.selectPage(page, lqw);
        // 3. 构建统一的响应对象
        PageResp<Playlist> pageResp = new PageResp<>();
        pageResp.setRecords(playListPage.getRecords());
        pageResp.setTotal(playListPage.getTotal());
        pageResp.setSize(playListPage.getSize());
        pageResp.setCurrent(playListPage.getCurrent());
        pageResp.setPages(playListPage.getPages());
        return pageResp;
    }

    @Override
    public void deletePlayList(long playListId) {
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getId, playListId);
        Playlist playlist = playListMapper.selectOne(lqw);
        if (playlist == null){
            throw new RuntimeException("歌单不存在");
        }

        if (playlist.getName().equals("我的喜欢")){
            throw new BusinessException("10001","歌单'我的喜欢'不允许删除");
        }

        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = loginUser.getUser().getId();
        Long playListUserId = playlist.getUserId();

        if (!playListUserId.equals(userId)) {
            throw new RuntimeException("您没有权限删除此歌单");
        }

        playListMapper.deleteById(playListId);

        String cacheKeyPattern = RedisCacheConstants.CACHE_KEY_PLAYLIST_DETAIL + playListId + ":*";
        stringRedisTemplate.delete(cacheKeyPattern);
    }

    @Override
    public Result<String> checkIn() {
        // 1. 获取当前登录的用户
        Long userId = SecurityUtil.getCurrentUserId();

        // 2. 获取日期,拼接前缀
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyy:MM"));
        String key = CHECK_IN_KEY + userId + ":" + keySuffix;

        // 3. 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        int offset = dayOfMonth - 1;

        // 4. 检查是否已签到
        Boolean isCheckedIn = stringRedisTemplate.opsForValue().getBit(key, offset);
        if (Boolean.TRUE.equals(isCheckedIn)) {
            return Result.ok("今日您已签到过了,明日再来吧~");
        }

        // 5. 执行签到
        stringRedisTemplate.opsForValue().setBit(key, offset, true);

        // 6. 设置过期时间（当前月月底）
        if (!stringRedisTemplate.hasKey(key)) {
            LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);
            long expireSeconds = Duration.between(now, endOfMonth).getSeconds();
            stringRedisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }

        // 7. 发送消息到消息队列
        //    1.更新用户签到时间  2.发放签到奖励
         int continuousSignInDays = getContinuousSignInDays().getData();
        int reward = 0;
        if (continuousSignInDays >= SIGN_IN_DAYS_7){
             reward = SIGN_IN_DAYS_7_REWORD;
        }else if (continuousSignInDays >= SIGN_IN_DAYS_5){
             reward = SIGN_IN_DAYS_5_REWORD;
        }else if (continuousSignInDays >= SIGN_IN_DAYS_3){
             reward = SIGN_IN_DAYS_3_REWORD;
        }else {
             reward = SIGN_IN_DAYS_1_REWORD;
        }

        SignInMessage message = new SignInMessage(true, reward, userId, now);

        rabbitTemplate.convertAndSend(SIGN_IN_SUCCESS_TOPIC_EXCHANGE, ROUTING_KEY_SIGN_IN_SUCCESS,  message);

        return Result.ok("签到成功");
    }

    @Override
    public Result<Integer> getContinuousSignInDays() {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        // 1. 获取今天的日期信息
        int dayOfMonth = now.getDayOfMonth(); // 今天是本月的第几天

        // 2. 构造Redis键
        String key = CHECK_IN_KEY + userId + ":" +
                now.format(DateTimeFormatter.ofPattern("yyyy:MM"));

        // 3. 从今天开始往前检查，直到找到未签到的天为止
        int continuousDays = 0;

        Boolean todaySigned = stringRedisTemplate.opsForValue().getBit(key, dayOfMonth - 1);
        if (Boolean.TRUE.equals(todaySigned)) {
            // 今天签到了，从今天开始检查
            // 从今天（偏移量=天数-1）开始往前检查
            for (int offset = dayOfMonth - 1; offset >= 0; offset--) {
                Boolean isSigned = stringRedisTemplate.opsForValue().getBit(key, offset);

                // 如果该天已签到，继续往前一天检查
                if (Boolean.TRUE.equals(isSigned)) {
                    continuousDays++;
                } else {
                    // 找到未签到的天，结束循环
                    break;
                }
            }


        }else  {
            //今天没签到从昨天开始检查
            // 从今天（偏移量=天数-1）开始往前检查
            for (int offset = dayOfMonth - 2; offset >= 0; offset--) {
                Boolean isSigned = stringRedisTemplate.opsForValue().getBit(key, offset);

                // 如果今天已签到，继续往前一天检查
                if (Boolean.TRUE.equals(isSigned)) {
                    continuousDays++;
                } else {
                    // 找到未签到的天，结束循环
                    break;
                }
            }

        }
        return Result.ok(continuousDays);
    }

    @Override
    public Result<CheckInStatusVO> getCheckInStatus() {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        // 1. 构造Redis键
        String key = CHECK_IN_KEY + userId + ":" +
                now.format(DateTimeFormatter.ofPattern("yyyy:MM"));

        // 2. 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        int todayOffset = dayOfMonth - 1; // 今天是第3天，偏移量就是2

        // 3. 检查今天是否已经签到
        Boolean todaySigned = stringRedisTemplate.opsForValue().getBit(key, todayOffset);
        boolean checkedIn = Boolean.TRUE.equals(todaySigned);

        // 4. 计算连续签到天数
        int continuousDays = getContinuousSignInDays().getData();

        // 5. 获取本月总签到天数（使用BITCOUNT命令）
        int monthCheckInDays = 0;

        // 方法1：使用RedisCallback执行BITCOUNT命令
        Long bitCount = stringRedisTemplate.execute(
                new RedisCallback<Long>() {
                    @Override
                    public Long doInRedis(RedisConnection connection) {
                        return connection.bitCount(key.getBytes());
                    }
                }
        );

        if (bitCount != null) {
            monthCheckInDays = bitCount.intValue();
        }

        // 方法2：或者使用更简洁的lambda写法
        // monthCheckInDays = stringRedisTemplate.execute(
        //     (RedisCallback<Long>) conn -> conn.bitCount(key.getBytes())
        // ).intValue();

        // 获取明日可得音浪数
        int tomorrowSignInReward = getTomorrowSignInReward().getData();
        // 获取当前用户音浪数
        Integer currentWaves = userMapper.selectMyWaves(userId);

        // 6. 封装返回结果
        CheckInStatusVO result = new CheckInStatusVO();
        result.setCheckedIn(checkedIn);
        result.setContinuousDays(continuousDays);
        result.setMonthCheckInDays(monthCheckInDays);
        result.setTomorrowSignInReward(tomorrowSignInReward);
        result.setCurrentWaves(currentWaves);

        return Result.ok(result);
    }



    // 获取热搜榜单
    @Override
    public Result<List<HotSearchVO>> getHotSearch() {
        try {
            // 1. 先查 Redis 缓存（手动反序列化）
            String cacheJson = stringRedisTemplate.opsForValue().get(RedisCacheConstants.CACHE_KEY_HOT_SEARCH);
            if (cacheJson != null && !cacheJson.isEmpty()) {
                // 缓存命中，直接反序列化为 List<HotSearchVO>
                List<HotSearchVO> hotSearchVOList = JSON.parseArray(cacheJson, HotSearchVO.class);
                return Result.ok(hotSearchVOList);
            }

            // 2. 缓存未命中，获取分布式锁（防止并发查库）
            RLock lock = redissonClient.getLock(RedisCacheConstants.LOCK_KEY_HOT_SEARCH);
            boolean isLockAcquired = false;
            try {
                // 尝试获取锁：最多等3秒，获取成功后30秒自动释放
                isLockAcquired = lock.tryLock(RedisCacheConstants.LOCK_WAIT_TIME, RedisCacheConstants.LOCK_EXPIRE_TTL, TimeUnit.SECONDS);
                if (!isLockAcquired) {
                    // 未获取到锁，返回空列表或友好提示（热搜为非核心功能，优先保证可用性）
                    return Result.ok(new ArrayList<>());
                    // 若需提示用户：return Result.fail("查询过于频繁，请稍后重试");
                }

                // 3. 双重检查缓存（防止其他线程已重建缓存）
                cacheJson = stringRedisTemplate.opsForValue().get(RedisCacheConstants.CACHE_KEY_HOT_SEARCH);
                if (cacheJson != null && !cacheJson.isEmpty()) {
                    List<HotSearchVO> hotSearchVOList = JSON.parseArray(cacheJson, HotSearchVO.class);
                    return Result.ok(hotSearchVOList);
                }

                // 4. 锁内查 DB 构建热搜榜单（原逻辑不变）
                LambdaQueryWrapper<HotSearch> queryWrapper = new LambdaQueryWrapper<HotSearch>()
                        .orderByDesc(HotSearch::getSortOrder) // 排序权重降序
                        .orderByDesc(HotSearch::getSearchCount) // 搜索量降序
                        .orderByDesc(HotSearch::getUpdateTime) // 更新时间降序
                        .last("LIMIT " + SystemConstants.HOT_SEARCH_LIMIT); // 限制条数

                List<HotSearch> hotSearchList = hotSearchMapper.selectList(queryWrapper);

                // 5. 转换为 VO
                List<HotSearchVO> hotSearchVOList = new ArrayList<>();
                for (HotSearch hotSearch : hotSearchList) {
                    HotSearchVO hotSearchVO = new HotSearchVO();
                    hotSearchVO.setKeyword(hotSearch.getKeyword());
                    hotSearchVO.setSearchCount(hotSearch.getSearchCount());
                    hotSearchVOList.add(hotSearchVO);
                }

                // 6. 手动序列化并写入 Redis（设置3分钟过期）
                String jsonStr = JSON.toJSONString(hotSearchVOList);
                stringRedisTemplate.opsForValue().set(RedisCacheConstants.CACHE_KEY_HOT_SEARCH, jsonStr, RedisCacheConstants.CACHE_TTL_SECONDS, TimeUnit.SECONDS);

                return Result.ok(hotSearchVOList);
            } finally {
                // 7. 释放锁（必须在 finally 中执行，避免锁泄露）
                if (isLockAcquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            // 异常时返回空列表，不影响用户核心操作
            return Result.fail("获取热搜榜单失败");
        }
    }

    // 搜索
    @Override
    public Result<PageResp<?>> search(SearchDTO searchDTO){
        try {
            if (searchDTO.getType() == null){
                return Result.fail("请选择搜索类型");
            }

            PageResp<?> pageResp = new PageResp<>();

            // 音乐
            if (searchDTO.getType().equals(SystemConstants.MUSIC_SEARCH_TYPE)){
                pageResp = musicService.searchMusic(searchDTO);
            }
            // 专辑
            if (searchDTO.getType().equals(SystemConstants.ALBUM_SEARCH_TYPE)){
                pageResp = musicService.searchAlbum(searchDTO);
            }
            // 音乐人
            if (searchDTO.getType().equals(SystemConstants.MUSICIAN_SEARCH_TYPE)){
                pageResp = musicianService.searchMusician(searchDTO);
            }
            // MV
            if (searchDTO.getType().equals(SystemConstants.MV_SEARCH_TYPE)){

            }
            // 用户
            if (searchDTO.getType().equals(SystemConstants.USER_SEARCH_TYPE)){
                pageResp = searchUser(searchDTO);
            }
            // 歌单
            if (searchDTO.getType().equals(SystemConstants.PLAYLIST_SEARCH_TYPE)){
                pageResp = playlistService.searchPlaylist(searchDTO);
            }

            // 写入搜索历史
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null){
                QueryWrapper<SearchHistory> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id", userId)
                        .eq("keyword", searchDTO.getKeyword());
                // 检查是否存在相同的搜索记录
                SearchHistory existingHistory = searchHistoryMapper.selectOne(queryWrapper);
                if (existingHistory != null) {
                    // 如果存在相同记录，更新搜索时间
                    existingHistory.setSearchTime(LocalDateTime.now());
                    searchHistoryMapper.updateById(existingHistory);
                } else {
                    // 如果不存在相同记录，写入新记录
                    // 写入搜索历史
                    SearchHistory searchHistory = new SearchHistory();
                    searchHistory.setUserId(userId);
                    searchHistory.setKeyword(searchDTO.getKeyword());
                    searchHistoryMapper.insert(searchHistory);
                }
            }

            // 写入热门搜索
            QueryWrapper<HotSearch> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("keyword", searchDTO.getKeyword());
            // 检查是否存在相同的热门搜索记录
            HotSearch existingHotSearch = hotSearchMapper.selectOne(queryWrapper);
            if (existingHotSearch != null) {
                // 如果存在相同记录，更新搜索计数和更新时间
                existingHotSearch.setSearchCount(existingHotSearch.getSearchCount() + 1);
                existingHotSearch.setUpdateTime(LocalDateTime.now());
                hotSearchMapper.updateById(existingHotSearch);
            } else {
                // 如果不存在相同记录，写入新记录
                // 写入热门搜索
                HotSearch hotSearch = new HotSearch();
                hotSearch.setKeyword(searchDTO.getKeyword());
                hotSearch.setSearchCount(1);
                hotSearch.setSortOrder(0);
                hotSearch.setUpdateTime(LocalDateTime.now());
                hotSearchMapper.insert(hotSearch);
            }

            return Result.ok(pageResp);
        }catch (Exception e){
            return Result.fail("搜索失败");
        }
    }
    // 搜索用户
    public PageResp<SearchUserVO> searchUser(SearchDTO dto) {
        // 1. 校验关键词
        if (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty()) {
            return new PageResp<>();
        }
        String keyword = "%" + dto.getKeyword().trim() + "%"; // 模糊匹配前缀后缀

        // 2. 构建分页参数
        Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        // 3. 执行查询
        IPage<User> userIPage = userMapper.selectPage(page,
                new LambdaQueryWrapper<User>()
                        .like(User::getUsername, keyword)
                        .eq(User::getUserStatus, 1));
        // 4. 转换为VO
        List<SearchUserVO> voList = new ArrayList<>();
        for(User user : userIPage.getRecords()) {
            SearchUserVO searchUserVO = new SearchUserVO();
            BeanUtils.copyProperties(user,searchUserVO);
            voList.add(searchUserVO);
        }

        PageResp<SearchUserVO> pageResp = new PageResp<>();
        pageResp.setRecords(voList);
        pageResp.setTotal(userIPage.getTotal());
        pageResp.setPages(userIPage.getPages());

        return pageResp;
    }

    // 获取搜索历史
    @Override
    public Result<List<SearchHistoryVO>> getSearchHistory(){
        try {
            // 1.获取当前用户
            Long userId = SecurityUtil.getCurrentUserId();

            // 2.查询搜索历史
            List<SearchHistory> searchHistoryList = searchHistoryMapper.selectList(
                    new LambdaQueryWrapper<SearchHistory>()
                            .eq(SearchHistory::getUserId, userId)
                            .orderByDesc(SearchHistory::getSearchTime)
            );

            // 3.转换为VO
            List<SearchHistoryVO> searchHistoryVOList = new ArrayList<>();
            for (SearchHistory searchHistory : searchHistoryList) {
                SearchHistoryVO searchHistoryVO = new SearchHistoryVO();
                BeanUtils.copyProperties(searchHistory, searchHistoryVO);
                searchHistoryVOList.add(searchHistoryVO);
            }

            return Result.ok(searchHistoryVOList);
        }catch (Exception e){
            return Result.fail("获取搜索历史失败");
        }
    }

    // 关键词联想
    @Override
    public Result<KeywordSuggestVO> getSuggestKeywords(String input) {
        try {
            // 1. 参数校验（原逻辑不变）
            if (input == null || input.trim().isEmpty() || input.length() > 20) {
                return Result.ok(new KeywordSuggestVO(List.of()));
            }
            input = input.trim();

            // 2. 构建缓存 Key 和锁 Key
            String cacheKey = RedisCacheConstants.CACHE_KEY_PREFIX + input;
            String lockKey = RedisCacheConstants.LOCK_KEY_PREFIX + input; // 锁 Key 与缓存 Key 一一对应

            // 3. 第一次查缓存（快速返回，避免无意义加锁）
            List<String> cachedSuggest = JSON.parseArray(stringRedisTemplate.opsForValue().get(cacheKey), String.class);
            if (cachedSuggest != null) {
                // 截取前10条返回
                List<String> resultList = cachedSuggest.subList(0, Math.min(cachedSuggest.size(), 10));
                return Result.ok(new KeywordSuggestVO(resultList));
            }

            // 4. 缓存未命中，获取分布式锁
            RLock lock = redissonClient.getLock(lockKey);
            boolean isLockAcquired = false;
            try {
                // 尝试获取锁：最多等3秒，获取成功后30秒自动释放（防止锁泄露）
                isLockAcquired = lock.tryLock(RedisCacheConstants.LOCK_WAIT_TIME, RedisCacheConstants.LOCK_EXPIRE_TTL, TimeUnit.SECONDS);
                if (!isLockAcquired) {
                    // 未获取到锁（其他线程正在重建缓存），返回空建议列表（或友好提示）
                    return Result.ok(new KeywordSuggestVO(List.of()));
                    // 若业务允许，可返回：return Result.fail("查询过于频繁，请稍后重试");
                }

                // 5. 双重检查缓存（防止其他线程已重建缓存）
                cachedSuggest = JSON.parseArray(stringRedisTemplate.opsForValue().get(cacheKey), String.class);
                if (cachedSuggest != null) {
                    stringRedisTemplate.expire(cacheKey, RedisCacheConstants.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    List<String> resultList = cachedSuggest.subList(0, Math.min(cachedSuggest.size(), 10));
                    return Result.ok(new KeywordSuggestVO(resultList));
                }

                // 6. 锁内查 DB 构建建议列表（原逻辑不变）
                String likePattern = input + "%";
                List<String> suggestList = new ArrayList<>();
                // 分别查询音乐名、歌手名、标签名（各取10条，后续去重）
                suggestList.addAll(musicMapper.selectSuggestMusicNames(likePattern, 10));
                suggestList.addAll(musicianMapper.selectSuggestMusicianNames(likePattern, 10));
                suggestList.addAll(musicTagMapper.selectSuggestTagNames(likePattern, 10));

                // 去重 + 截取前10条
                List<String> finalSuggest = suggestList.stream()
                        .distinct()
                        .limit(10)
                        .collect(Collectors.toList());

                // 7. 存入缓存（空结果1分钟过期，非空5分钟，原逻辑不变）
                long expireSeconds = finalSuggest.isEmpty() ? 60 : RedisCacheConstants.CACHE_EXPIRE_SECONDS;
                stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(finalSuggest), expireSeconds, TimeUnit.SECONDS);

                // 8. 返回结果
                return Result.ok(new KeywordSuggestVO(finalSuggest));
            } finally {
                // 9. 释放锁（必须在 finally 中执行，避免锁泄露）
                if (isLockAcquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            // 异常时返回空建议列表，不影响用户使用
            return Result.ok(new KeywordSuggestVO(List.of()));
        }
    }

    @Override
    public Result<Integer> getTomorrowSignInReward() {
        Long userId = SecurityUtil.getCurrentUserId();
        Integer data = getContinuousSignInDays().getData();
        int tomorrowSignInDays = data + 1;
        int reward = 0;
        if (tomorrowSignInDays >= SIGN_IN_DAYS_7) {
            reward = SIGN_IN_DAYS_7_REWORD;
        }else if (tomorrowSignInDays >= SIGN_IN_DAYS_5 ) {
            reward = SIGN_IN_DAYS_5_REWORD;
        } else if (tomorrowSignInDays >= SIGN_IN_DAYS_3 ) {
            reward = SIGN_IN_DAYS_3_REWORD;
        } else if (tomorrowSignInDays >= SIGN_IN_DAYS_1 ){
            reward = SIGN_IN_DAYS_1_REWORD;
        }
        return Result.ok(reward);
    }
}