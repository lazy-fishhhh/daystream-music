package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.future.music.domain.dto.MusicPlayReportDTO;
import com.future.music.domain.vo.MusicDetailVO;
import com.future.music.domain.vo.MusicPlayVO;
import entity.dto.MusicDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.Music;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IMusicService extends IService<Music> {
    Result<Void> saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover, MultipartFile audio1, MultipartFile audio2, MultipartFile audio3, MultipartFile audio4 );

    Result<Void> removeMusic(Long musicId);

    void downloadMusic(Long musicId, Integer quality, HttpServletResponse response);

    Result<MusicDetailVO> echoMusic(Long musicId);

    Result<Void> updateMusic(Long musicId, MusicDTO musicDTO,
                       MultipartFile lyric, MultipartFile cover, Map<Integer, MultipartFile> audioMap);

    Result<MusicDetailVO> getOneMusic(Long musicId);

    PageResp<MusicDetailVO> searchMusic(SearchDTO dto);

    Result<MusicPlayVO> playMusic(Long musicId, Integer quality);

    Result<Void> reportPlayStatus(MusicPlayReportDTO dto);

}
