package com.future.music.service;


import com.future.music.domain.vo.SearchMusicianVO;
import entity.dto.SearchDTO;
import entity.pojo.util.PageResp;

public interface IMusicianService {

    PageResp<SearchMusicianVO> searchMusician(SearchDTO dto);

}
