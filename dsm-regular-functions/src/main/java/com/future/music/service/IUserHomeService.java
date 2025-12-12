package com.future.music.service;

import com.future.music.domain.vo.DailyListenDataVO;
import com.future.music.domain.vo.MusicTagRatioVO;
import com.future.music.domain.vo.MusicVO;
import com.future.music.domain.vo.PlaylistVO;
import com.future.music.domain.vo.AlbumVO;
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

    Result<PageResp<AlbumVO>> getMyCollectAlbums(PageReq pageReq);

    Result<List<DailyListenDataVO>> getDailyListenData();

    /**
     * 获取本周不同标签音乐的占比
     * @return 标签占比数据列表
     */
    Result<List<MusicTagRatioVO>> getWeeklyTagRatio();

    Result<PageResp<List<PlaylistVO>>> getMyLikeSongsPlaylist(PageReq pageReq);
}
