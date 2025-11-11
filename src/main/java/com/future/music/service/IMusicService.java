package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.future.music.entity.dto.MusicDTO;
import com.future.music.entity.pojo.music.Music;
import com.future.music.entity.pojo.util.Result;
import org.springframework.web.multipart.MultipartFile;

public interface IMusicService extends IService<Music> {
    Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover);

}
