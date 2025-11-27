package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.vo.MusicDetailVO;
import com.future.music.domain.vo.MusicFilterTagVO;
import com.future.music.domain.vo.PlaylistDetailVO;
import com.future.music.domain.vo.PlaylistVO;
import com.future.music.mapper.MusicTagMapper;
import com.future.music.mapper.PlayListMapper;
import com.future.music.mapper.PlaylistMusicMapper;
import com.future.music.service.IMusicService;
import com.future.music.service.IPlaylistService;
import constants.MusicTagTypeConstants;
import entity.pojo.music.Music;
import entity.pojo.music.MusicTag;
import entity.pojo.music.Playlist;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaylistServiceImpl implements IPlaylistService {

    @Resource
    private IMusicService musicService;

    @Resource
    private PlayListMapper playlistMapper;

    @Resource
    private PlaylistMusicMapper playlistMusicMapper;

    @Resource
    private MusicTagMapper musicTagMapper;



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

        // 3. 封装成VO（视图对象，用于返回给前端）
        PlaylistDetailVO detailVO = new PlaylistDetailVO();
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
        pageResult.setPages((int) musicPage.getPages());

        detailVO.setMusicDetailVOList(pageResult);

        return Result.ok(detailVO);
    }

    /**
     * 根据标签筛选歌单
     * @param tags 标签ID列表（从请求参数获取）
     * @return 统一返回格式（成功/失败 + 数据）
     */
    @Override
    public Result<PageResp<PlaylistVO>> filterPlaylistByTagIds(List<Long> tags, int pageNum, int pageSize) {
        if (tags == null || tags.isEmpty()) {
            return Result.fail("标签不能为空");
        }

        Page<Playlist>  page = new Page<>(pageNum, pageSize);
        IPage<Playlist> playlistPage = playlistMapper.selectPlaylistsByTagIds(page, tags);

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
        pageResult.setPages((int) playlistPage.getPages());

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

}
