package com.future.music.service;


import entity.dto.PlayListDTO;
import entity.pojo.music.Playlist;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;

public interface IUserService {
    void createPlayList(PlayListDTO playListDTO);

    void updatePlayList(PlayListDTO playList);

    PageResp<Playlist> getPlayList(PageReq pageReq);
}
