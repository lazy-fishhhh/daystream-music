package com.future.music.service;


import com.future.music.domain.vo.MusicDetailVO;
import com.future.music.domain.vo.PlaylistDetailVO;
import entity.dto.PlayListDTO;
import entity.pojo.music.Playlist;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

public interface IUserService {
    void createPlayList(PlayListDTO playListDTO);

    void updatePlayList(PlayListDTO playList);

    PageResp<Playlist> getPlayList(PageReq pageReq);

    void deletePlayList(long playListDTO);

    Result<String> checkIn();

    Result<Integer> getContinuousSignInDays();

    Result<PlaylistDetailVO> getPlaylistDetail(Long playlistId, int pageNum, int pageSize);
}
