package com.future.music.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.future.music.mapper.MusicianMapper;
import com.future.music.service.IMusicianService;
import entity.dto.PlayListDTO;
import entity.pojo.music.PlayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
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
        playList.setCreatedTime(LocalDateTime.now());
        playList.setUpdatedTime(LocalDateTime.now());
        musicianMapper.CreatePlayList(playList);
    }

    @Override
    public void updatePlayList(PlayListDTO dto) {
        LambdaQueryWrapper<PlayList> lqw = new LambdaQueryWrapper<>();
        lqw.eq(PlayList::getId,dto.getId());
        PlayList playList1= musicianMapper.selectOne(lqw);
        if (playList1 == null){
            throw new RuntimeException("歌单不存在");
        }

//        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
//        Long userId = loginUser.getUser().getId();
//        Long playListUserId = playList1.getUserId();
//        if(playListUserId != userId){
//            throw new RuntimeException("您没有权限修改此歌单");
//        }
//
//        PlayList playList = new PlayList();
//        playList.setName(dto.getName());
//        playList.setCover(dto.getCover());
//        playList.setDescription(dto.getDescription());
//        playList.setIsPublic(dto.getIsPublic());
//        playList.setUpdatedTime(LocalDateTime.now());
//        musicianMapper.updateById(playList);


    }

    @Override
    public PlayList getPlayList() {
//        LoginUser loginUser = (LoginUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        Long userId  = loginUser.getUser().getId();
        //泛型 <PlayList> 明确指定了该查询条件是针对 PlayList 实体类的
//        LambdaQueryWrapper<PlayList> lqw = new LambdaQueryWrapper<>();
//
//        lqw.eq(PlayList::getUserId, userId);
//        musicianMapper.selectList()
        return null;
    }
}
