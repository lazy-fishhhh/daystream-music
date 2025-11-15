package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;

import entity.dto.MusicDTO;
import entity.pojo.music.Music;
import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IMusicService extends IService<Music> {
    Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover, MultipartFile audio);

    Result removeMusic(Long musicId);

    void downloadMusic(Long musicId, Integer quality, HttpServletResponse response);

    Result<Music> echoMusic(Long musicId);

    Result updateMusic(Long musicId, MusicDTO musicDTO,
                       MultipartFile lyric, MultipartFile cover, MultipartFile audio);

    Result<Music> getOneMusic(Long musicId);

}
