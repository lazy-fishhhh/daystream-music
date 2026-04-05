package com.future.music.service;

import com.future.music.domain.vo.AlbumVO;
import com.future.music.entity.vo.MusicDetailVO;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

import java.util.List;

public interface IMusicianHomeService {
    Result<PageResp<List<MusicDetailVO>>> getMyMusic(Long musicianId, Integer pageNum, Integer pageSize);
    Result<AlbumVO> getAlbumDetail(Long albumId);
}
