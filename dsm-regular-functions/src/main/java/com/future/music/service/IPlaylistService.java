package com.future.music.service;

import com.future.music.domain.vo.*;
import com.future.music.entity.dto.PlaylistFilterDTO;
import entity.dto.SearchDTO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

import java.util.List;

public interface IPlaylistService {

    Result<PlaylistDetailVO> getPlaylistDetail(Long playlistId, int pageNum, int pageSize);

    Result<List<CollectorVO>> getPlaylistCollectors(String playlistId, int pageNum, int pageSize);

    Result<PageResp<PlaylistVO>> filterPlaylistByTagIds(PlaylistFilterDTO filterDTO);

    Result<MusicFilterTagVO> getAllFilterTags();

    PageResp<SearchPlaylistVO> searchPlaylist(SearchDTO dto);
    
    Result<PageResp<List<PlaylistVO>>> queryMyPlaylistByCreateTime(Long userId, int pageNum, int pageSize);
    
    Result<PageResp<List<PlaylistVO>>> queryMyPlaylistByCollect(Long userId, int pageNum, int pageSize);

    Result<String> addSongToPlaylist(Long playlistId, Long songId);

    Result<String> batchAddSongToPlaylist(Long playlistId, List<Long> songIds);

    Result<String> removeSongFromPlaylist(Long playlistId, Long songId);

    Result<String> batchRemoveSongFromPlaylist(Long playlistId, List<Long> songIds);

    Result<PageResp<MusicVO>> querySongsByPlaylist(Long playlistId, int pageNum, int pageSize);

}

