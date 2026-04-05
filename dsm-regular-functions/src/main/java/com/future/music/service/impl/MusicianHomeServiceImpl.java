package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.vo.AlbumVO;
import com.future.music.domain.vo.MusicVO;
import com.future.music.entity.vo.MusicDetailVO;

import com.future.music.mapper.MusicMapper;
import com.future.music.mapper.RegularAlbumMapper;
import com.future.music.service.IMusicianHomeService;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MusicianHomeServiceImpl implements IMusicianHomeService {
    private final MusicMapper musicMapper;
    private final RegularAlbumMapper albumMapper;

    @Override
    public Result<PageResp<List<MusicDetailVO>>> getMyMusic(Long musicianId, Integer pageNum, Integer pageSize) {

        // 创建分页对象
        Page<MusicDetailVO> page = new Page<>(pageNum, pageSize);
        IPage<MusicDetailVO> musicVOIPage = musicMapper.selectMusicVOsByMusicianIdPage(page, musicianId);

        // 处理标签字符串到List<String>的转换
        List<MusicDetailVO> musicDetailVOs = musicVOIPage.getRecords();
        for (MusicDetailVO musicDetailVO : musicDetailVOs) {
            // XML查询返回的tagName字段是用逗号分隔的字符串
            String tagNameStr = musicDetailVO.getTagName();
            if (tagNameStr != null && !tagNameStr.isEmpty()) {
                String[] tagsArray = tagNameStr.split(",");
                musicDetailVO.setTags(List.of(tagsArray));
            } else {
                musicDetailVO.setTags(List.of());
            }
            // 清空临时tagName字段
            musicDetailVO.setTagName(null);
        }

        // 封装分页结果
        PageResp<List<MusicDetailVO>> pageResp = new PageResp<>();
        pageResp.setRecords(List.of(musicDetailVOs));
        pageResp.setTotal(musicVOIPage.getTotal());
        pageResp.setSize(musicVOIPage.getSize());
        pageResp.setCurrent(musicVOIPage.getCurrent());
        pageResp.setPages(musicVOIPage.getPages());

        return Result.ok(pageResp);
    }

    @Override
    public Result<AlbumVO> getAlbumDetail(Long albumId) {
        // 查询专辑详情
        AlbumVO albumVO = albumMapper.selectAlbumDetailById(albumId);
        if (albumVO == null) {
            return Result.fail("专辑不存在");
        }

        // 查询专辑下的所有歌曲
        List<MusicVO> musicList = albumMapper.selectMusicsByAlbumId(albumId);
        albumVO.setMusicList(musicList);

        return Result.ok(albumVO);
    }
}
