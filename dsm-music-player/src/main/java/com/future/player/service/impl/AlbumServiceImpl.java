package com.future.player.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.exception.BusinessException;
import com.future.login.util.SecurityUtil;

import com.future.player.entity.dto.AlbumDTO;
import com.future.player.entity.vo.AlbumVO;
import com.future.player.mapper.AlbumMapper;
import com.future.player.mapper.MPlayerMapper;

import com.future.player.mapper.PlayerMusicianMapper;
import com.future.player.service.IAlbumService;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.music.Musician;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static code.BusinessErrorCode.MUSIC_NOT_EXIST;
import static code.BusinessErrorCode.PERSISTENCE_DENY;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements IAlbumService {

    @Resource
    private AlbumMapper albumMapper;

    @Resource
    private PlayerMusicianMapper playerMusicianMapper;


    private final MPlayerMapper mPlayerMapper;

    @Autowired
    private BaseMapper<Musician> musicianMapper;

    @Override
    public Result<Void> createAlbum(AlbumDTO albumDTO) {
        try {
            Album album = new Album();
            // 拷贝DTO属性到实体
            BeanUtils.copyProperties(albumDTO, album);

            // 设置默认值
            if (album.getStatus() == null) {
                album.setStatus(1); // 默认上架
            }
            // 直接设置musicianId为当前用户ID
            album.setMusicianId(SecurityUtil.getCurrentUserId());
            if (album.getReleaseDate() == null) {
                album.setReleaseDate(LocalDateTime.now()); // 默认当前时间
            }

            albumMapper.insert(album);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("创建专辑失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Void> updateAlbum(AlbumDTO albumDTO) {
        try {
            // 验证专辑是否存在
            Album existingAlbum = albumMapper.selectById(albumDTO.getId());
            if (existingAlbum == null) {
                return Result.fail("专辑不存在");
            }

            boolean isAuthor = existingAlbum.getMusicianId().equals(SecurityUtil.getCurrentUserId());
            if(!isAuthor){
                return Result.fail("您没有权限修改此专辑");
            }
            Album album = new Album();
            BeanUtils.copyProperties(albumDTO, album);

            albumMapper.updateById(album);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("修改专辑失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteAlbum(Long albumId) {
        try {
            // 验证专辑是否存在
            Album existingAlbum = albumMapper.selectById(albumId);
            if (existingAlbum == null) {
                return Result.fail("专辑不存在");
            }
            boolean isAuthor = existingAlbum.getMusicianId().equals(SecurityUtil.getCurrentUserId());
            if(!isAuthor){
                return Result.fail("您没有权限修改此专辑");
            }
            albumMapper.deleteById(albumId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除专辑失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PageResp<AlbumVO>> queryMyAlbum(int pageNum, int pageSize) {
        try {
            // 获取当前用户ID
            Long userId = getCurrentUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }

            // 1. 构建MyBatis-Plus分页对象
            Page<AlbumVO> page = new Page<>(pageNum, pageSize);
            // 2. 调用Mapper分页查询
            IPage<AlbumVO> albumPage = albumMapper.selectAlbumByUserId(page, userId);

            // 3. 转换为通用分页响应对象
            PageResp<AlbumVO> pageResp = convertIPageToPageResp(albumPage);

            return Result.ok(pageResp);
        } catch (Exception e) {
            return Result.fail("查询我的专辑失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PageResp<AlbumVO>> queryOtherAlbum(Long userId, int pageNum, int pageSize) {
        try {
            if (userId == null) {
                return Result.fail("用户ID不能为空");
            }

            // 1. 构建分页对象
            Page<AlbumVO> page = new Page<>(pageNum, pageSize);
            // 2. 分页查询指定用户的专辑
            IPage<AlbumVO> albumPage = albumMapper.selectAlbumByUserId(page, userId);

            // 3. 过滤公开专辑（status=1）
            List<AlbumVO> publicAlbums = albumPage.getRecords().stream()
                    .filter(album -> album.getStatus() != null && album.getStatus() == 1)
                    .collect(Collectors.toList());

            // 4. 重新计算分页参数（过滤后的数据）
            PageResp<AlbumVO> pageResp = new PageResp<>();
            pageResp.setRecords(publicAlbums);
            pageResp.setTotal(publicAlbums.size());
            pageResp.setSize(pageSize);
            pageResp.setCurrent(pageNum);
            pageResp.setPages(publicAlbums.size() == 0 ? 0 : (long) Math.ceil((double) publicAlbums.size() / pageSize));

            return Result.ok(pageResp);
        } catch (Exception e) {
            return Result.fail("查询该用户的公开专辑失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PageResp<AlbumVO>> queryAllAlbum(String key, int pageNum, int pageSize) {
        try {
            // 1. 构建分页对象
            Page<AlbumVO> page = new Page<>(pageNum, pageSize);
            // 2. 调用Mapper分页查询所有上架专辑（key为null时查全部）
            IPage<AlbumVO> albumPage = albumMapper.searchAlbumByKey(page, key);

            // 3. 转换为通用分页响应
            PageResp<AlbumVO> pageResp = convertIPageToPageResp(albumPage);

            return Result.ok(pageResp);
        } catch (Exception e) {
            return Result.fail("查询所有公开专辑失败：" + e.getMessage());
        }
    }

    @Override
    public Result<AlbumVO> getAlbumDetail(Long albumId) {
        try {
            if (albumId == null) {
                return Result.fail("专辑ID不能为空");
            }

            // 查询专辑详情
            Album album = albumMapper.selectById(albumId);
            if (album == null) {
                return Result.fail("专辑不存在");
            }

            // 检查专辑访问权限
            if (album.getStatus() != 1) { // 非公开专辑
                Long currentUserId = getCurrentUserId();
                if (currentUserId == null) {
                    return Result.fail("未登录，无法访问非公开专辑");
                }
                // 查询专辑所属音乐人
                Musician musician = playerMusicianMapper.selectById(album.getMusicianId());
                if (musician == null || !Objects.equals(musician.getUserId(), currentUserId)) {
                    return Result.fail("非公开专辑，您无权查看");
                }
            }

            // 转换为VO返回
            return Result.ok(convertToAlbumVO(album));
        } catch (Exception e) {
            return Result.fail("获取专辑详情失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PageResp<AlbumVO>> queryPublicAlbum(int pageNum, int pageSize) {
        try {
            // 参数校验
            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            Page<AlbumVO> page = new Page<>(pageNum, pageSize);
            IPage<AlbumVO> albumVOIPage = albumMapper.selectAllPublicAlbum(page);
            PageResp<AlbumVO> pageResp = convertIPageToPageResp(albumVOIPage);

            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("查询公开专辑分页失败", e); // 打印异常日志
            return Result.fail("查询专辑失败，请稍后重试"); // 友好提示
        }
    }

    /**
     * 将MyBatis-Plus的IPage转换为通用PageResp
     * @param iPage MyBatis-Plus分页对象
     * @return 通用分页响应
     */
    private PageResp<AlbumVO> convertIPageToPageResp(IPage<AlbumVO> iPage) {
        PageResp<AlbumVO> pageResp = new PageResp<>();
        pageResp.setRecords(iPage.getRecords()); // 数据列表
        pageResp.setTotal(iPage.getTotal());     // 总记录数
        pageResp.setSize(iPage.getSize());       // 每页大小
        pageResp.setCurrent(iPage.getCurrent()); // 当前页码
        pageResp.setPages(iPage.getPages());     // 总页数
        return pageResp;
    }

    /**
     * 将Album实体转换为AlbumVO
     */
    private AlbumVO convertToAlbumVO(Album album) {
        if (album == null) {
            return null;
        }
        AlbumVO albumVO = new AlbumVO();
        albumVO.setId(album.getId());
        albumVO.setAlbumName(album.getAlbumName());
        // 查询音乐人名称
        Musician musician = playerMusicianMapper.selectById(album.getMusicianId());
        if (musician != null) {
            albumVO.setMusicianName(musician.getStageName());
        }
        albumVO.setCoverUrl(album.getCoverUrl());
        albumVO.setReleaseDate(album.getReleaseDate());
        albumVO.setIntroduction(album.getIntroduction());
        albumVO.setStatus(album.getStatus());
        albumVO.setCollectCount(album.getCollectCount());
        albumVO.setCommentCount(album.getCommentCount());
        return albumVO;
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 关联歌曲到专辑
     * @param albumId 专辑ID
     * @param songIds 歌曲ID列表
     * @return 操作结果
     */
    @Transactional
    @Override
    public Result<String> bindSongsToAlbum(Long albumId, List<Long> songIds) {
        try {
            // 1. 验证专辑是否存在
            Album album = albumMapper.selectById(albumId);
            if (album == null) {
                return Result.fail("专辑不存在");
            }

            // 2. 验证当前用户是否是专辑的创建者
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.fail("用户未登录");
            }

            // 3. 验证当前用户是否是该专辑的音乐人
            Musician musician = musicianMapper.selectOne(new LambdaQueryWrapper<Musician>()
                    .eq(Musician::getUserId, currentUserId)
                    .eq(Musician::getStatus, 1));
            if (musician == null || !musician.getId().equals(album.getMusicianId())) {
                return Result.fail("您没有权限将歌曲关联到该专辑");
            }

            int count = 0;
            // 4. 更新歌曲的albumId字段
            for (Long songId : songIds) {
                // 验证歌曲是否存在且属于当前用户
                Music music = mPlayerMapper.selectById(songId);
                if (music == null) {
                    throw new BusinessException(MUSIC_NOT_EXIST.getCode(),"歌曲不存在，id:" + songId);
                }
                if (!music.getMusicianId().equals(musician.getId())) {
                    throw new BusinessException(PERSISTENCE_DENY.getCode(),"您没有权限将歌曲关联到该专辑:" + music.getMusicName());
                }
                // 更新歌曲的专辑ID
                music.setAlbumId(albumId);
                mPlayerMapper.updateById(music);
                count++;
            }
            LambdaUpdateWrapper<Album> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(Album::getMusicCount, album.getMusicCount() + count);
            return Result.ok("歌曲关联到专辑成功");
        } catch (Exception e) {
            log.error("关联歌曲到专辑失败: {}", e.getMessage(), e);
            return Result.fail("关联歌曲到专辑失败: " + e.getMessage());
        }
    }
}