package com.future.music.service;


import com.future.music.domain.vo.*;
import entity.dto.PlayListDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.Playlist;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

import java.util.List;

public interface IUserService {
    void createPlayList(PlayListDTO playListDTO);

    void updatePlayList(PlayListDTO playList);

    PageResp<Playlist> getPlayList(PageReq pageReq);

    void deletePlayList(long playListDTO);

    Result<String> checkIn();

    Result<Integer> getContinuousSignInDays();

    Result<PageResp<?>> search(SearchDTO searchDTO);

    Result<List<SearchHistoryVO>> getSearchHistory();

}
