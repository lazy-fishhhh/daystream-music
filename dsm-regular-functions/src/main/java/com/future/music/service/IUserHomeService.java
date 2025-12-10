package com.future.music.service;

import com.future.music.domain.vo.MusicVO;
import com.future.music.domain.vo.PlaylistVO;
import com.future.music.domain.dto.UserDetailDTO;
import entity.pojo.music.Album;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import social.domain.vo.UserInfoVO;

import java.util.List;

public interface IUserHomeService {
    Result<String> updateUserInfo(UserDetailDTO userInfoVO);

    Result<UserInfoVO> getUserInfo();

    Result<PageResp<List<MusicVO>>> getMyLikeSongs(PageReq pageReq);

    Result<PageResp<List<PlaylistVO>>> getMyCollectSongLists(PageReq pageReq);

    Result<PageResp<List<Album>>> getMyCollectAlbums(PageReq pageReq);

    Result<List<Integer>> getDailyListenCount();
}
