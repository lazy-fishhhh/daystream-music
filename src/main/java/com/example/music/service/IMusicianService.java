package com.example.music.service;

import com.example.music.entity.dto.PlayListDTO;
import org.springframework.stereotype.Service;


public interface IMusicianService {
    void createPlayList(PlayListDTO playListDTO);
}
