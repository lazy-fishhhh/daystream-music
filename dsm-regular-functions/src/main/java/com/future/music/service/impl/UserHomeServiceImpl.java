package com.future.music.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.dto.UserDetailDTO;
import com.future.music.domain.vo.MusicVO;
import com.future.music.domain.vo.PlaylistVO;

import com.future.music.mapper.LikeRecordMapper;
import com.future.music.mapper.MusicMapper;
import com.future.music.mapper.PlaylistMapper;
import com.future.music.mapper.UserCollectionMapper;
import com.future.music.service.IMusicService;
import com.future.music.service.IUserHomeService;
import com.future.player.mapper.AlbumMapper;
import entity.pojo.info.User;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.music.Playlist;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.UserCollection;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import social.domain.vo.UserInfoVO;

import java.util.List;
import java.util.stream.Collectors;

import java.security.Security;
import java.time.LocalDateTime;

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
    private final AlbumMapper albumMapper;
    private final IMusicService musicService;

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



            BeanUtil.copyProperties(dto, existUser, true); // 第三个参数为true表示忽略空值


            // 5. 执行更新操作
            userMapper.updateById(existUser);


            // 7. 日志记录（操作成功）
            log.info("更新用户信息成功，用户ID：{}，更新内容：{}", userId, StrUtil.hide(dto.toString(), 0, 50));

            // 8. 成功返回
            return Result.ok("个人信息更新成功");

        } catch (IllegalArgumentException e) {
            // 参数校验异常
            log.error("更新用户信息失败，参数校验不通过：{}，用户ID：{}", e.getMessage(),
                    dto != null ? userId : "未知");
            return Result.fail(e.getMessage());
        } catch (BusinessException e) {
            // 自定义业务异常
            log.error("更新用户信息失败，业务异常：{}，用户ID：{}", e.getMessage(),
                    dto != null ? userId : "未知");
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 全局异常捕获
            log.error("更新用户信息失败，系统异常，用户ID：{}",
                    dto != null ? userId: "未知", e);
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
            return Result.ok(userInfoVO);
        } catch (BusinessException e) {
            throw new BusinessException(e.getCode(), e.getMessage());
        } catch (Exception e){
            throw new BusinessException(USER_INFO_ERROR.getCode(),"用户信息获取失败");
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
                    .eq(UserCollection::getTargetType, 3)
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
    public Result<PageResp<List<Album>>> getMyCollectAlbums(PageReq pageReq) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }

            // 构建查询条件
            LambdaQueryWrapper<UserCollection> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserCollection::getUserId, userId)
                    .eq(UserCollection::getTargetType, 4)
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
            List<Album> albumList = albumIds.stream()
                    .map(albumMapper::selectById)
                    .filter(album -> album != null)
                    .toList();

            // 封装分页结果
            PageResp<List<Album>> pageResp = new PageResp<>();
            pageResp.setRecords(List.of(albumList));
            pageResp.setTotal(collectionPage.getTotal());
            pageResp.setPages(collectionPage.getPages());

            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("获取用户收藏的专辑失败：", e);
            return Result.fail("获取用户收藏的专辑失败");
        }
    }
}
