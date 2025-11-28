package com.future.music.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.vo.SearchMusicianVO;
import com.future.music.mapper.MusicianMapper;
import com.future.music.service.IMusicianService;
import entity.dto.SearchDTO;
import entity.pojo.music.Album;
import entity.pojo.music.Musician;
import entity.pojo.util.PageResp;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicianServiceImpl implements IMusicianService {
    private  final MusicianMapper musicianMapper;

    @Override
    public PageResp<SearchMusicianVO> searchMusician(SearchDTO dto) {
        // 1. 校验关键词
        if (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty()) {
            return new PageResp<>();
        }
        String keyword = "%" + dto.getKeyword().trim() + "%"; // 模糊匹配前缀后缀

        // 2. 构建分页参数
        Page<Musician> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        // 3. 执行查询
        IPage<Musician> musicianPage = musicianMapper.selectPage(page,
                new LambdaQueryWrapper<Musician>()
                        .like(Musician::getStageName, keyword)
                        .eq(Musician::getStatus, 1));
        // 4. 转换为VO
        List<SearchMusicianVO> voList = new ArrayList<>();
        for(Musician musician : musicianPage.getRecords()) {
            SearchMusicianVO searchMusicianVO = new SearchMusicianVO();
            BeanUtils.copyProperties(musician,searchMusicianVO);
            voList.add(searchMusicianVO);
        }

        PageResp<SearchMusicianVO> pageResp = new PageResp<>();
        pageResp.setRecords(voList);
        pageResp.setTotal(musicianPage.getTotal());
        pageResp.setPages(musicianPage.getPages());

        return pageResp;
    }

}
