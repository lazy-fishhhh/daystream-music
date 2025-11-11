package com.example.music.service;

import com.example.music.entity.dto.PlayListDTO;
import com.example.music.entity.pojo.music.PlayList;


public interface IMusicianService {
    void createPlayList(PlayListDTO playListDTO);

    void updatePlayList(PlayListDTO playList);

    PlayList getPlayList();
}
