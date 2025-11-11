package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;

import entity.dto.MusicDTO;
import entity.pojo.music.Music;
import entity.pojo.util.Result;
import org.springframework.web.multipart.MultipartFile;

public interface IMusicService extends IService<Music> {
    Result<Void> saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover);

}
