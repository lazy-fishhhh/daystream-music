package com.future.music.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.dto.UserDetailDTO;
import com.future.music.domain.dto.DailyListenCountDTO;
import com.future.music.domain.vo.*;

import com.future.music.mapper.LikeRecordMapper;
import com.future.music.mapper.MusicMapper;
import com.future.music.mapper.PlaylistMapper;
import com.future.music.mapper.UserCollectionMapper;
import com.future.music.mapper.UserListenHistoryMapper;
import com.future.music.mapper.RegularAlbumMapper;
import com.future.music.service.IMusicService;
import com.future.music.service.IUserHomeService;
import com.future.music.websocket.ChatEndPoint;
import com.future.player.mapper.AlbumMapper;
import constants.MusicConstants;
import constants.SystemConstants;
import entity.pojo.info.User;
import entity.pojo.music.Album;
import entity.pojo.music.Playlist;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.UserCollection;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import social.domain.RoleEnum;
import social.domain.vo.UserInfoVO;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static code.BusinessErrorCode.USER_INFO_ERROR;
import static code.BusinessErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHomeServiceImpl implements IUserHomeService {
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final UserCollectionMapper userCollectionMapper;
    private final MusicMapper musicMapper;
    private final PlaylistMapper playlistMapper;
    private final RegularAlbumMapper regularAlbumMapper;
    private final IMusicService musicService;
    private final MinIoServiceImpl minIoService;
    private final UserListenHistoryMapper userListenHistoryMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<String> updateUserInfo(UserDetailDTO dto) {
        // 1. 基础参数校验
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            log.warn("更新用户信息失败，用户未登录");
            return Result.fail("用户未登录，更新失败");
        }
        try {
            // 2. 业务逻辑校验
            User existUser = userMapper.selectById(userId);
            if (existUser == null) {
                log.warn("更新用户信息失败，用户不存在，用户ID：{}", userId);
                return Result.fail("用户不存在，更新失败");
            }

            // 头像上传
            if (dto.getAvatarFile() != null && !dto.getAvatarFile().isEmpty()) {
                // 校验头像格式
                boolean isAvatarAllowed = minIoService.isAllowedFile(dto.getAvatarFile(), "picture");
                if (!isAvatarAllowed) {
                    log.warn("更新头像失败，文件格式不合法，用户ID：{}", userId);
                    return Result.fail("头像仅支持jpg/jpeg/png/gif格式");
                }
                String avatarUrl = minIoService.uploadFile(dto.getAvatarFile());
                if (avatarUrl == null) {
                    log.warn("更新头像失败，MinIO上传失败，用户ID：{}", userId);
                    return Result.fail("头像上传失败，请重试");
                }
                dto.setAvatar(avatarUrl);
                String oldAvatar = existUser.getAvatar();
                if (oldAvatar != null && !oldAvatar.isEmpty()) {
                    minIoService.deleteFile(oldAvatar);
                }
            }

            //背景图片上传
            if (dto.getBackgroundImageFile() != null && !dto.getBackgroundImageFile().isEmpty()) {
                // 校验背景图格式
                boolean isBgAllowed = minIoService.isAllowedFile(dto.getBackgroundImageFile(), "picture");
                if (!isBgAllowed) {
                    log.warn("更新背景图片失败，文件格式不合法，用户ID：{}", userId);
                    return Result.fail("背景图片仅支持jpg/jpeg/png/gif格式");
                }
                String bgImageUrl = minIoService.uploadFile(dto.getBackgroundImageFile());
                if (bgImageUrl == null) {
                    log.warn("更新背景图片失败，MinIO上传失败，用户ID：{}", userId);
                    return Result.fail("背景图片上传失败，请重试");
                }
                dto.setBackgroundImage(bgImageUrl);
                String oldBgImage = existUser.getBackgroundImage();
                if (oldBgImage != null && !oldBgImage.isEmpty()) {
                    minIoService.deleteFile(oldBgImage);
                }
            }

            BeanUtil.copyProperties(dto, existUser, "gender");

            if (dto.getGender() != null) {
                if ("男".equals(dto.getGender())) {
                    existUser.setGender(1);
                } else if ("女".equals(dto.getGender())) {
                    existUser.setGender(2);
                } else if ("未知".equals(dto.getGender())) {
                    existUser.setGender(0);
                } else {
                    log.warn("更新用户信息失败，用户性别参数错误，用户ID：{}", userId);
                    return Result.fail("用户性别参数错误，更新失败");
                }
            }

            // 执行更新操作
            userMapper.updateById(existUser);

            // 日志记录
            log.info("更新用户信息成功，用户ID：{}，更新内容：{}", userId, StrUtil.hide(dto.toString(), 0, 50));

            // 成功返回
            return Result.ok("个人信息更新成功");

        } catch (IllegalArgumentException e) {
            log.error("更新用户信息失败，参数校验不通过：{}，用户ID：{}", e.getMessage(), userId);
            return Result.fail(e.getMessage());
        } catch (BusinessException e) {
            log.error("更新用户信息失败，业务异常：{}，用户ID：{}", e.getMessage(), userId);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("更新用户信息失败，系统异常，用户ID：{}", userId, e);
            return Result.fail("系统异常，更新失败，请联系管理员");
        }
    }

    @Override
    public Result<UserInfoVO> getUserInfo() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null){
                return Result.fail("用户未登录");
            }
            User currentUser = userMapper.selectById(userId);
            if (currentUser == null){
                throw new BusinessException(USER_NOT_FOUND.getCode(),"用户不存在");
            }
            UserInfoVO userInfoVO = new UserInfoVO();

            userInfoVO.setId(currentUser.getId());
            userInfoVO.setUsername(currentUser.getUsername());
            userInfoVO.setAvatar(currentUser.getAvatar());
            if (currentUser.getGender() == 1){
                userInfoVO.setGender("男");
            }else if (currentUser.getGender() == 2){
                userInfoVO.setGender("女");
            }else {
                userInfoVO.setGender("未知");
            }

            userInfoVO.setIntroduction(currentUser.getIntroduction());


            boolean isVip = false;
            if (currentUser.getVipExpireTime() != null) {
                isVip = currentUser.getVipExpireTime().isAfter(LocalDateTime.now());
            }
            userInfoVO.setIsVip(isVip);
            userInfoVO.setVipExpireTime(currentUser.getVipExpireTime());
            userInfoVO.setEmail(currentUser.getEmail());
            userInfoVO.setPhone(currentUser.getPhone());
            userInfoVO.setTotalPoint(currentUser.getTotalPoint());
            userInfoVO.setWalletBalance(currentUser.getWalletBalance());
            userInfoVO.setCreatedTime(String.valueOf(currentUser.getCreatedTime()));
            userInfoVO.setLastCheckinTime(currentUser.getLastCheckinTime());
            userInfoVO.setLastLoginTime(currentUser.getLastLoginTime());
            userInfoVO.setUpdatedTime(currentUser.getUpdatedTime());
            userInfoVO.setBackgroundImage(currentUser.getBackgroundImage());
            userInfoVO.setFollowCount(currentUser.getFollowCount());
            userInfoVO.setFansCount(currentUser.getFansCount());

            // 获取在线状态
            boolean userOnline = ChatEndPoint.isUserOnline(userId);
            userInfoVO.setOnlineStatus(userOnline ? 1 : 0);

            // 获取用户身份
            Integer i = userMapper.selectRoleId(userId);
            if (i == null){
                throw new BusinessException(USER_NOT_FOUND.getCode(),"未查询到用户身份");
            }
            RoleEnum roleEnum = RoleEnum.valueOf(i);
            userInfoVO.setUserRole(roleEnum.getDoc());

            // 获取当前音乐人的所有歌曲点赞总数
            Long l = likeRecordMapper.selectLikeCountByUserId(userId);

            userInfoVO.setLikeCount(Math.toIntExact(l));
            return Result.ok(userInfoVO);
        } catch (BusinessException e) {
            throw new BusinessException(e.getCode(), e.getMessage());
        } catch (Exception e){
            log.error("用户信息获取失败" + e.getMessage(),e);
            throw new BusinessException(USER_INFO_ERROR.getCode(),"用户信息获取失败" + e.getMessage());
        }
    }

    @Override
    public Result<PageResp<List<MusicVO>>> getMyLikeSongs(PageReq pageReq) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }

            LambdaQueryWrapper<LikeRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LikeRecord::getUserId, userId)
                    .eq(LikeRecord::getTargetType, 1)
                    .eq(LikeRecord::getIsCanceled, 0)
                    .orderByDesc(LikeRecord::getCreatedTime);

            // 分页查询点赞记录
            Page<LikeRecord> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
            Page<LikeRecord> likeRecordPage = likeRecordMapper.selectPage(page, queryWrapper);

            // 获取音乐ID列表
            List<Long> musicIds = likeRecordPage.getRecords().stream()
                    .map(LikeRecord::getTargetId)
                    .toList();

            // 查询音乐详情
            List<MusicVO> musicVOList = musicIds.stream()
                    .map(musicService::getMusicVO)
                    .toList();

            // 封装分页结果
            PageResp<List<MusicVO>> pageResp = new PageResp<>();
            pageResp.setRecords(List.of(musicVOList));
            pageResp.setTotal(likeRecordPage.getTotal());
            pageResp.setPages(likeRecordPage.getPages());

            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("获取用户喜欢的歌曲失败：", e);
            return Result.fail("获取用户喜欢的歌曲失败");
        }
    }

    @Override
    public Result<PageResp<List<PlaylistVO>>> getMyCollectSongLists(PageReq pageReq) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }

            LambdaQueryWrapper<UserCollection> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserCollection::getUserId, userId)
                    .eq(UserCollection::getTargetType, 1)
                    .eq(UserCollection::getIsCanceled, 0)
                    .orderByDesc(UserCollection::getCollectedTime);

            // 分页查询收藏记录
            Page<UserCollection> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
            Page<UserCollection> collectionPage = userCollectionMapper.selectPage(page, queryWrapper);

            // 获取歌单ID列表
            List<Long> playlistIds = collectionPage.getRecords().stream()
                    .map(UserCollection::getTargetId)
                    .toList();

            // 查询歌单详情
            List<PlaylistVO> playlistVOList = playlistIds.stream()
                    .map(playlistId -> {
                        Playlist playlist = playlistMapper.selectById(playlistId);
                        if (playlist == null) {
                            return null;
                        }
                        PlaylistVO playlistVO = new PlaylistVO();
                        BeanUtil.copyProperties(playlist, playlistVO);
                        return playlistVO;
                    })
                    .filter(playlistVO -> playlistVO != null)
                    .toList();

            // 封装分页结果
            PageResp<List<PlaylistVO>> pageResp = new PageResp<>();
            pageResp.setRecords(List.of(playlistVOList));
            pageResp.setTotal(collectionPage.getTotal());
            pageResp.setPages(collectionPage.getPages());

            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("获取用户收藏的歌单失败：", e);
            return Result.fail("获取用户收藏的歌单失败");
        }
    }

    @Override
    public Result<PageResp<List<AlbumVO>>> getMyCollectAlbums(PageReq pageReq) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }

            // 构建查询条件
            LambdaQueryWrapper<UserCollection> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserCollection::getUserId, userId)
                    .eq(UserCollection::getTargetType, 2)
                    .eq(UserCollection::getIsCanceled, 0)
                    .orderByDesc(UserCollection::getCollectedTime);

            // 分页查询收藏记录
            Page<UserCollection> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
            Page<UserCollection> collectionPage = userCollectionMapper.selectPage(page, queryWrapper);

            // 获取专辑ID列表
            List<Long> albumIds = collectionPage.getRecords().stream()
                    .map(UserCollection::getTargetId)
                    .toList();

            // 查询专辑详情
            List<AlbumVO> albumVOList = new ArrayList<>();
            if (!albumIds.isEmpty()) {
                albumVOList = regularAlbumMapper.selectAlbumVOsWithMusicByIds(albumIds);
            }

            // 封装分页结果
            PageResp<List<AlbumVO>> pageResp = new PageResp<>();
            pageResp.setRecords(List.of(albumVOList));
            pageResp.setTotal(collectionPage.getTotal());
            pageResp.setPages(collectionPage.getPages());

            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("获取用户收藏的专辑失败：", e);
            return Result.fail("获取用户收藏的专辑失败");
        }
    }

    @Override
    public Result<List<MusicTagRatioVO>> getWeeklyTagRatio() {
        try {
            // 1. 获取当前登录用户ID
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                log.warn("获取本周标签占比失败，用户未登录");
                return Result.fail("用户未登录，获取失败");
            }

            // 2. 生成本周的周标识
            String weekId = generateWeekId(LocalDate.now());
            String cacheKey = MusicConstants.USER_LISTEN_TAG_WEEK + userId + ":" + weekId;

            // 3. 从Redis中获取本周的所有标签数据
            Map<Object, Object> tagCountMap = stringRedisTemplate.opsForHash().entries(cacheKey);
            if (tagCountMap.isEmpty()) {
                log.info("本周无标签数据，用户ID：{}", userId);

                return Result.ok(new ArrayList<>());
            }

            // 4. 计算总播放次数
            long totalCount = 0;
            for (Object value : tagCountMap.values()) {
                try {
                    totalCount += Long.parseLong(value.toString());
                } catch (NumberFormatException e) {
                    // 跳过无效数据
                    log.warn("标签数据错误：{}", value);
                }
            }

            // 5. 计算每个标签的占比
            List<MusicTagRatioVO> tagRatioList = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : tagCountMap.entrySet()) {
                String tagName = entry.getKey().toString();
                long count;
                try {
                    count = Long.parseLong(entry.getValue().toString());
                } catch (NumberFormatException e) {
                    // 跳过无效数据
                    log.info("跳过无效数据");
                    continue;
                }
                double ratio = totalCount > 0 ? (double) count / totalCount : 0;
                tagRatioList.add(new MusicTagRatioVO(tagName, count, ratio));
            }

            // 6. 按占比降序排序
            tagRatioList.sort((a, b) -> Double.compare(b.getRatio(), a.getRatio()));

            // 7. 返回结果
            log.info("获取用户本周标签占比成功，用户ID：{}", userId);
            return Result.ok(tagRatioList);
        } catch (Exception e) {
            log.error("获取用户本周标签占比失败：", e);
            return Result.fail("获取失败，请稍后重试");
        }
    }

    /**
     * 生成指定日期的周标识（周一为周初，周日为周末）
     * @param date 播放日期（null 则取当前日期）
     * @return 周标识，如 2025-49
     */
    public String generateWeekId(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        // 调整为本周一（确保整周统一标识）
        LocalDate mondayOfWeek = targetDate.with(DayOfWeek.MONDAY);
        return mondayOfWeek.format(SystemConstants.WEEK_FORMATTER);
    }
    @Override
    public Result<List<DailyListenDataVO>> getDailyListenData() {
        try {
            // 1. 获取当前登录用户ID
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                log.warn("获取本周每日听歌次数失败，用户未登录");
                return Result.fail("用户未登录，获取失败");
            }

            // 2. 计算本周的开始和结束日期
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            // 3. 查询数据库获取本周的听歌记录
            List<DailyListenCountDTO> listenData =
                    userListenHistoryMapper.getWeeklyDailyListenCount(userId, startOfWeek, endOfWeek);

            // 4. 将查询结果转换为Map，方便查找每日的听歌数据
            Map<LocalDate, DailyListenCountDTO> listenDataMap = listenData.stream()
                    .collect(Collectors.toMap(
                            DailyListenCountDTO::getDay,
                            Function.identity()
                    ));

            // 5. 生成本周每天的日期列表，并填充对应日期的听歌次数和时长
            List<DailyListenDataVO> dailyData = new ArrayList<>();
            LocalDate currentDate = startOfWeek;
            while (!currentDate.isAfter(endOfWeek)) {
                DailyListenCountDTO dto = listenDataMap.getOrDefault(currentDate, null);
                if (dto != null) {
                    dailyData.add(new DailyListenDataVO(dto.getCount(), dto.getSeconds()));
                } else {
                    dailyData.add(new DailyListenDataVO(0, 0L));
                }
                currentDate = currentDate.plusDays(1);
            }

            // 6. 返回结果
            log.info("获取用户本周每日听歌数据成功，用户ID：{}，数据：{}", userId, dailyData);
            return Result.ok(dailyData);
        } catch (Exception e) {
            log.error("获取用户本周每日听歌数据失败：", e);
            return Result.fail("获取失败，请稍后重试");
        }
    }
}
