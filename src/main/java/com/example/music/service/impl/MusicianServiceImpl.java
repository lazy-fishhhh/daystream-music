package com.example.music.service.impl;


import com.example.music.entity.dto.PlayListDTO;
import com.example.music.entity.pojo.music.PlayList;
import com.example.music.mapper.MusicianMapper;
import com.example.music.service.IMusicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;


@RequiredArgsConstructor
public class MusicianServiceImpl implements IMusicianService {
    private  final MusicianMapper musicianMapper;
    @Override
    public void createPlayList(PlayListDTO playListDTO) {
//        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
//        Long userId = loginUser.getUser().getId();
        PlayList playList = new PlayList();
//        playList.setUserId(userId);
        playList.setName(playListDTO.getName());
        playList.setCover(playListDTO.getCover());
        playList.setDescription(playListDTO.getDescription());
        playList.setIsPublic(playListDTO.getIsPublic());

        musicianMapper.CreatePlayList(playList);
    }
}
