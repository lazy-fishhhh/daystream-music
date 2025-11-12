package com.future.music.service;


import entity.dto.PlayListDTO;
import entity.pojo.music.PlayList;

public interface IMusicianService {
    void createPlayList(PlayListDTO playListDTO);

    void updatePlayList(PlayListDTO playList);

    PlayList getPlayList();
}
