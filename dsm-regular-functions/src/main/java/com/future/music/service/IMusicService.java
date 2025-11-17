package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.future.music.domain.vo.MusicDetailVO;
import entity.dto.MusicDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.Music;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IMusicService extends IService<Music> {
    Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover, Map<Integer, MultipartFile> audioMap);

    Result removeMusic(Long musicId);

    void downloadMusic(Long musicId, Integer quality, HttpServletResponse response);

    Result<MusicDetailVO> echoMusic(Long musicId);

    Result updateMusic(Long musicId, MusicDTO musicDTO,
                       MultipartFile lyric, MultipartFile cover, Map<Integer, MultipartFile> audioMap);

    Result<MusicDetailVO> getOneMusic(Long musicId);

    PageResp<MusicDetailVO> searchMusic(SearchDTO dto);



}
