package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.entity.dto.MusicDTO;
import com.example.music.entity.pojo.music.Music;
import com.example.music.entity.pojo.util.Result;
import org.springframework.web.multipart.MultipartFile;

public interface IMusicService extends IService<Music> {
    Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover);

}
