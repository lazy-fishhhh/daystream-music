package com.future.music.service;

import com.future.music.domain.vo.*;
import entity.dto.SearchDTO;
import entity.pojo.music.MusicTag;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

import java.util.List;

public interface IPlaylistService {

    Result<PlaylistDetailVO> getPlaylistDetail(Long playlistId, int pageNum, int pageSize);

    Result<PageResp<PlaylistVO>> filterPlaylistByTagIds(List<Long> tags, int pageNum, int pageSize);

    Result<MusicFilterTagVO> getAllFilterTags();

    PageResp<SearchPlaylistVO> searchPlaylist(SearchDTO dto);

}
