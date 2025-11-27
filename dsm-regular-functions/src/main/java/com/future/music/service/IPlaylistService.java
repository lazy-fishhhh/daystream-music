package com.future.music.service;

import com.future.music.domain.vo.MusicFilterTagVO;
import com.future.music.domain.vo.PlaylistDetailVO;
import com.future.music.domain.vo.PlaylistVO;
import entity.pojo.music.MusicTag;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

import java.util.List;

public interface IPlaylistService {

    Result<PlaylistDetailVO> getPlaylistDetail(Long playlistId, int pageNum, int pageSize);

    Result<PageResp<PlaylistVO>> filterPlaylistByTagIds(List<Long> tags, int pageNum, int pageSize);

    Result<MusicFilterTagVO> getAllFilterTags();

}
