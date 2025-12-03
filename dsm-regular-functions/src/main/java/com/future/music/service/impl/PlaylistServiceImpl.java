package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.vo.*;
import com.future.music.mapper.MusicTagMapper;
import com.future.music.mapper.PlaylistMapper;
import com.future.music.mapper.PlaylistMusicMapper;
import com.future.music.service.IMusicService;
import com.future.music.service.IPlaylistService;
import constants.MusicTagTypeConstants;
import entity.dto.PlaylistFilterDTO;
import entity.dto.SearchDTO;
import entity.pojo.info.User;
import entity.pojo.music.*;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaylistServiceImpl implements IPlaylistService {

    @Resource
    private IMusicService musicService;

    @Resource
    private PlaylistMapper playlistMapper;

    @Resource
    private PlaylistMusicMapper playlistMusicMapper;

    @Resource
    private MusicTagMapper musicTagMapper;

    @Resource
    private UserMapper userMapper;



    /**
     * 根据歌单ID查询歌单详情（包含基本信息和关联歌曲ID）
     * @param playlistId 歌单ID
     * @return 歌单详情（含歌曲ID列表），若不存在则返回null
     */
    @Override
    public Result<PlaylistDetailVO> getPlaylistDetail(Long playlistId, int pageNum, int pageSize) {
        if (playlistId == null || playlistId <= 0) {
            return Result.fail("歌单ID出错");
        }

        // 1. 查询歌单基本信息
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            return Result.fail("歌单不存在");
        }

        // 2. 查询歌单包含的歌曲ID列表（关联表查询）
        Page<Music> page = new Page<>(pageNum,pageSize);
        IPage<Music> musicPage = playlistMusicMapper.selectMusicsByPlaylistId(page, playlistId);
        //  查询歌单创建者
        String creator = playlistMapper.selectPlaylistCreator(playlistId);
        String avatar = playlistMapper.selectPlaylistCreatorAvator(playlistId);

        // 3. 封装成VO（视图对象，用于返回给前端）
        PlaylistDetailVO detailVO = new PlaylistDetailVO();
        detailVO.setAvatar(avatar);
        detailVO.setCreator(creator);
        // 复制基本属性（使用BeanUtils简化代码）
        BeanUtils.copyProperties(playlist, detailVO);

        List<MusicDetailVO> voList = new ArrayList<>();
        for (Music music : musicPage.getRecords()) {
            Result<MusicDetailVO> result = musicService.getOneMusic(music.getId());
            if (result.getErrorMsg() != null) {
                return Result.fail(result.getErrorMsg());
            }
            MusicDetailVO vo = result.getData();
            voList.add(vo);
        }

        PageResp<MusicDetailVO> pageResult = new PageResp<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(musicPage.getTotal());
        pageResult.setPages(musicPage.getPages());

        detailVO.setMusicDetailVOList(pageResult);

        return Result.ok(detailVO);
    }

    /**
     * 根据标签筛选歌单
     * @param filterDTO 歌单筛选请求参数
     * @return 统一返回格式（成功/失败 + 数据）
     */
    @Override
    public Result<PageResp<PlaylistVO>> filterPlaylistByTagIds(PlaylistFilterDTO filterDTO) {
        if (filterDTO == null || filterDTO.getTags() == null || filterDTO.getTags().isEmpty()) {
            return Result.fail("标签不能为空");
        }

        // 获取分页参数，设置默认值
        Integer pageNum = filterDTO.getPageNum() != null ? filterDTO.getPageNum() : 1;
        Integer pageSize = filterDTO.getPageSize() != null ? filterDTO.getPageSize() : 10;
        
        Page<Playlist> page = new Page<>(pageNum, pageSize);
        IPage<Playlist> playlistPage = playlistMapper.selectPlaylistsByTagIds(page, 
                                                                           filterDTO.getTags(), 
                                                                           filterDTO.getTags().size(),
                                                                           filterDTO.getStartTime(), 
                                                                           filterDTO.getEndTime(), 
                                                                           filterDTO.getMinCollect(), 
                                                                           filterDTO.getMaxCollect());

        // 转换为vo
        List<PlaylistVO> voList = new ArrayList<>();
        for (Playlist playlist : playlistPage.getRecords()) {
            PlaylistVO vo = new PlaylistVO();
            BeanUtils.copyProperties(playlist, vo);
            voList.add(vo);
        }

        // 封装分页信息
        PageResp<PlaylistVO> pageResult = new PageResp<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(playlistPage.getTotal());
        pageResult.setPages(playlistPage.getPages());

        return Result.ok(pageResult);
    }

    /**
     * 获取所有可选筛选标签（流派、心情、场景、乐器）
     * @return 分类后的筛选标签列表
     */
    @Override
    public Result<MusicFilterTagVO> getAllFilterTags() {
        MusicFilterTagVO result = new MusicFilterTagVO();

        // 1. 按新标签类型查询（1=流派、2=心情、3=场景、4=乐器）
        List<MusicTag> genreTags = musicTagMapper.selectTagsByType(MusicTagTypeConstants.GENRE);
        List<MusicTag> moodTags = musicTagMapper.selectTagsByType(MusicTagTypeConstants.MOOD);
        List<MusicTag> sceneTags = musicTagMapper.selectTagsByType(MusicTagTypeConstants.SCENE);
        List<MusicTag> instrumentTags = musicTagMapper.selectTagsByType(MusicTagTypeConstants.INSTRUMENT);

        // 2. 转换为VO
        result.setGenreTags(convertToVO(genreTags));
        result.setMoodTags(convertToVO(moodTags));
        result.setSceneTags(convertToVO(sceneTags));
        result.setInstrumentTags(convertToVO(instrumentTags));

        return Result.ok(result);
    }
    /**
     * MusicTag 转 MusicTagVO
     */
    private List<MusicFilterTagVO.MusicTagVO> convertToVO(List<MusicTag> musicTags) {
        return musicTags.stream().map(tag -> {
            MusicFilterTagVO.MusicTagVO vo = new MusicFilterTagVO.MusicTagVO();
            BeanUtils.copyProperties(tag, vo);
            return vo;
        }).collect(Collectors.toList());
    }


    @Override
    public PageResp<SearchPlaylistVO> searchPlaylist(SearchDTO dto){
        // 1. 校验关键词
        if (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty()) {
            return new PageResp<>();
        }
        String keyword = "%" + dto.getKeyword().trim() + "%"; // 模糊匹配前缀后缀

        // 2. 构建分页参数
        Page<Playlist> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        // 3. 构建多条件查询（音乐名、标签名、音乐人名字 模糊匹配）
        QueryWrapper<Playlist> queryWrapper = new QueryWrapper<>();

        queryWrapper.and(wrapper -> wrapper
                        .eq("m.music_name", keyword)        // 条件1：音乐名
                        .or().like("p.playlist_name", keyword)  // 条件2：歌单名
                        .or().eq("mt.tag_name", keyword)    // 条件3：标签名
                )
                .eq("p.is_public", 1); // 全局过滤：所有条件都需满足公开=1

        // 4. 执行多表联查+分页+排序（SQL已含ORDER BY play_count DESC）
        IPage<Playlist> playlistPage = playlistMapper.searchPlaylistsByMultiCondition(page, queryWrapper);

        // 5. 转换为PlaylistVO并填充关联信息（点赞状态、音频列表等）
        List<SearchPlaylistVO> voList = new ArrayList<>();
        for (Playlist playlist : playlistPage.getRecords()) {
            SearchPlaylistVO vo = new SearchPlaylistVO();
            BeanUtils.copyProperties(playlist, vo);

            // 5.4 关联用户名（通过userId查user表）
            User user = userMapper.selectById(playlist.getUserId());
            if (user != null) {
                vo.setUserName(user.getUsername());
            }

            voList.add(vo);
        }

        // 6. 封装分页结果
        PageResp<SearchPlaylistVO> pageResult = new PageResp<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(playlistPage.getTotal());
        pageResult.setPages(playlistPage.getPages());

        return pageResult;
    }

    @Override
    public Result<PageResp<List<PlaylistVO>>> queryMyPlaylistByCreateTime(int pageNum, int pageSize) {

        Long userId = SecurityUtil.getCurrentUserId();

        if (userId == null) {
            return Result.fail("用户未登录");
        }

        // 构建分页参数
        Page<Playlist> page = new Page<>(pageNum, pageSize);

        // 构建查询条件：按创建时间降序
        LambdaQueryWrapper<Playlist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Playlist::getUserId, userId)
                .orderByDesc(Playlist::getCreatedTime);

        // 执行查询
        IPage<Playlist> playlistPage = playlistMapper.selectPage(page, queryWrapper);

        // 转换为VO
        PageResp<List<PlaylistVO>> pageResult = convertToPageResp(playlistPage);

        return Result.ok(pageResult);
    }

    @Override
    public Result<PageResp<List<PlaylistVO>>> queryMyPlaylistByCollect(int pageNum, int pageSize) {
        // 获取当前用户ID
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.fail("用户未登录");
        }

        // 构建分页参数
        Page<Playlist> page = new Page<>(pageNum, pageSize);

        // 构建查询条件：按收藏量降序
        LambdaQueryWrapper<Playlist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Playlist::getUserId, userId)
                .orderByDesc(Playlist::getCollectCount);

        // 执行查询
        IPage<Playlist> playlistPage = playlistMapper.selectPage(page, queryWrapper);

        PageResp<List<PlaylistVO>> pageResult = convertToPageResp(playlistPage);

        return Result.ok(pageResult);
    }

    @NotNull
    private static PageResp<List<PlaylistVO>> convertToPageResp(IPage<Playlist> playlistPage) {
        // 转换为VO
        List<PlaylistVO> voList = new ArrayList<>();
        for (Playlist playlist : playlistPage.getRecords()) {
            PlaylistVO vo = new PlaylistVO();
            BeanUtils.copyProperties(playlist, vo);
            voList.add(vo);
        }

        // 封装分页结果
        PageResp<List<PlaylistVO>> pageResult = new PageResp<>();
        pageResult.setRecords(List.of(voList));
        pageResult.setTotal(playlistPage.getTotal());
        pageResult.setPages(playlistPage.getPages());
        return pageResult;
    }


}

