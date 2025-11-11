package com.future.music.service;

import com.future.music.entity.dto.PlayListDTO;
import com.future.music.entity.pojo.music.PlayList;


public interface IMusicianService {
    void createPlayList(PlayListDTO playListDTO);

    void updatePlayList(PlayListDTO playList);

    PlayList getPlayList();
}
