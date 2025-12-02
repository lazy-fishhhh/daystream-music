package admin.service.impl;

import admin.domain.dto.MusicianDTO;
import admin.domain.vo.MusicianVO;
import admin.mapper.AdminMusicianMapper;
import admin.service.AdminMusicianService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j

public class AdminMusicianServiceImpl implements AdminMusicianService {
    @Autowired
    private AdminMusicianMapper adminMusicianMapper;
    @Override
    public Result<PageResp<MusicianVO>> getMusicianList(MusicianDTO musicianDTO) {
        //构建mybatis分页参数
        Page<MusicianVO> page = new Page<>(musicianDTO.getPageNum(), musicianDTO.getPageSize());
        IPage<MusicianVO> musicianList = adminMusicianMapper.getMusicianList(page, musicianDTO.getStatus(), musicianDTO.getKey());
        PageResp<MusicianVO> pageResp = new PageResp<>();
        pageResp.setRecords(musicianList.getRecords());
        pageResp.setTotal(musicianList.getTotal());
        pageResp.setPages(musicianList.getPages());
        pageResp.setCurrent(musicianList.getCurrent());
        pageResp.setSize(musicianList.getSize());
        return Result.ok(pageResp);
    }
}
