package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.future.music.domain.dto.MusicPlayDTO;
import com.future.music.domain.dto.MusicPlayReportDTO;
import com.future.music.domain.vo.*;
import entity.dto.MusicDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.Music;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IMusicService extends IService<Music> {
    Result<Void> saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover, Map<Integer, MultipartFile> audioMap);

    Result<Void> removeMusic(Long musicId);

    void downloadMusic(Long musicId, Integer quality, HttpServletResponse response);

    Result<EchoMusicVO> echoMusic(Long musicId);

    Result<Void> updateMusic(Long musicId, MusicDTO musicDTO,
                       MultipartFile lyric, MultipartFile cover, Map<Integer, MultipartFile> audioMap);

    MusicVO getMusicVO(Long musicId);

    Result<MusicDetailVO> getOneMusic(Long musicId);

    PageResp<MusicVO> searchMusic(SearchDTO dto);
    PageResp<SearchAlbumVO> searchAlbum(SearchDTO dto);

    Result<MusicPlayVO> playMusic(MusicPlayDTO dto);

    Result<Void> reportPlayStatus(MusicPlayReportDTO dto);

    Result<PageResp<MusicVO>> filterMusicByTags(List<Long> tagIds, int pageNum, int pageSize);

}
