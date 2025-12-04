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
import com.future.music.domain.vo.*;
import com.future.music.mapper.*;
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
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.future.music.code.PlaylistErrorCode.PLAYLIST_NAME_EXIST;
import static com.future.music.util.constants.CHECK_IN_KEY;


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



    //构造方法注入
    private final PlaylistMapper playListMapper;
    private final PlaylistTagRelationMapper playlistTagRelationMapper;
    @Autowired
    private UserMapper userMapper;

    @Transactional
    @Override
    public void createPlayList(PlayListDTO playListDTO) {
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getName, playListDTO.getName());
        Playlist playlist = playListMapper.selectOne(lqw);
        if (playlist != null){
            throw new BusinessException(PLAYLIST_NAME_EXIST.getCode(),"歌单名称已存在,不允许重复");
        }

        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = loginUser.getUser().getId();
        Playlist playList = new Playlist();
        playList.setUserId(userId);
        playList.setName(playListDTO.getName());
        playList.setCover(playListDTO.getCover());
        playList.setDescription(playListDTO.getDescription());
        playList.setIsPublic(playListDTO.getIsPublic());
        playList.setCreatedTime(LocalDateTime.now());
        playList.setUpdatedTime(LocalDateTime.now());
        playListMapper.insert(playList);

         PlaylistTagRelation playListTagRelation = new PlaylistTagRelation();
         playListTagRelation.setPlaylistId(playList.getId());
         playListTagRelation.setTagId((long) playListDTO.getTagId());
         playlistTagRelationMapper.insert(playListTagRelation);
    }

    @Transactional
    @Override
    public void updatePlayList(PlayListDTO dto) {
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getId,dto.getId());
        Playlist playlist1 = playListMapper.selectOne(lqw);
        if (playlist1 == null){
            throw new RuntimeException("歌单不存在");
        }

        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = loginUser.getUser().getId();
        Long playListUserId = playlist1.getUserId();

        if (!playListUserId.equals(userId)) {
            throw new RuntimeException("您没有权限修改此歌单");
        }


        // 创建 PlayList 对象并设置 ID 属性
        Playlist playlist = new Playlist();
        //updateById是通过id来更新的，所以必须设置id
        playlist.setId(dto.getId());
        playlist.setName(dto.getName());
        playlist.setCover(dto.getCover());
        playlist.setDescription(dto.getDescription());
        playlist.setIsPublic(dto.getIsPublic());
        playlist.setUpdatedTime(LocalDateTime.now());

        // 使用 MyBatis-Plus 的 updateById 方法
        playListMapper.updateById(playlist);

        PlaylistTagRelation playListTagRelation = new PlaylistTagRelation();
        playListTagRelation.setPlaylistId(dto.getId());
        playListTagRelation.setTagId((long) dto.getTagId());
        playlistTagRelationMapper.updateById(playListTagRelation);

        String cacheKeyPattern = RedisCacheConstants.CACHE_KEY_PLAYLIST_DETAIL + dto.getId() + ":*";
        stringRedisTemplate.delete(cacheKeyPattern);
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

        // 2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3. 拼接Key
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = CHECK_IN_KEY + userId + ":" + keySuffix;
        // 4. 获取今天是本月的第几天
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int offset = dayOfMonth - 1; // 偏移量，从0开始所以要-1

        // 5. 检查用户是否已经签到
        // Redis的getBit命令：获取键对应偏移量的bit值（1=已签到，0=未签到）
        // 若键不存在，返回null（需通过Boolean.TRUE.equals判断，避免空指针）
        Boolean isCheckedIn = stringRedisTemplate.opsForValue().getBit(key, offset);

        // 当key不存在时，getBit返回null，此时需要特殊处理

        if (Boolean.TRUE.equals(isCheckedIn)) {
            return Result.fail(400, "今日您已签到过了,明日再来吧~");
        }

        // 6. 写入Redis SETBIT key offset 1
        // 将当前偏移量的bit值设为1（表示已签到）
        // SETBIT命令：原子操作，确保并发场景下不会重复签到
        stringRedisTemplate.opsForValue().setBit(key, offset, true);

//        // 7. 设置过期时间-到本年年底，避免数据永久存储
//        if (!stringRedisTemplate.hasKey(key)) {
//            // 计算本年底最后一天
//            LocalDate endOfYear = LocalDate.now().withMonth(12).withDayOfMonth(31);
//            // 计算从今天到年底的天数（加1确保包含年底当天），设置为过期时间
//            long expireDays = ChronoUnit.DAYS.between(LocalDate.now(), endOfYear) + 1;
//            stringRedisTemplate.expire(key, expireDays, TimeUnit.DAYS);
//        }

        return Result.ok("签到成功");
    }

    @Override
    public Result<Integer> getContinuousSignInDays() {
        // 1.获取当前用户
        Long userId = SecurityUtil.getCurrentUserId();

        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 3. 拼接日期
        String key = CHECK_IN_KEY + userId + ":" + keySuffix;

        // 4. 获取今天是本月的第几天
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int offset = dayOfMonth - 1; // 偏移量，从0开始所以要-1

        // 5. 获取本月截至今天为止的所有签到记录，返回的是一个十进制数字
        // BITFIELD key GET uoffset offset
        // uoffset：unsigned offset，无符号偏移量，用于指定要都到哪里
        // offset：指定从哪里开始读取
        // 从0开始读取，读取到offset位置

        //多个子命令多返回值，所以返回一个List
        List<Long> signInRecord = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(offset))//GET TYPE
                        .valueAt(0)//从0开始读取，读取到offset位置的bit值    //OFFSET
        );
        if(signInRecord== null || signInRecord.isEmpty()){
            //没有任何签到结果
            return Result.ok(0);
        }
        //list里只有一个元素，就是签到记录，所以get(0)
        Long num = signInRecord.get(0);
        if(num == null || num == 0){
            //没有任何签到结果
            return Result.ok(0);
        }

        // 6.循环遍历所有签到记录，判断是否连续签到
        int continuousDays = 0;
        // 从右向左遍历，因为是从0开始读取，所以要从右向左遍历
        // 不是1，说明签到中断
        // 是1，说明连续签到
        while ((num & 1) != 0) {
            // 判断当前位(最右边的位)是否为1
            continuousDays++;
            // 右移一位，检查下一位
            // 整数进行右移一位（num >> 1）时，二进制数整体向右移动
            // 原来的最右边的第 0 位会被丢弃，原来的第 1 位会变成新的第 0 位（最右边）
            num = num >> 1;
        }

        return Result.ok(continuousDays);
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

                // 5. 转换为 VO（原逻辑不变）
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
}