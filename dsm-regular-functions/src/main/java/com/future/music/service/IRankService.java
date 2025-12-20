package com.future.music.service;

import com.future.music.domain.vo.RankHomeVO;
import entity.pojo.util.Result;

public interface IRankService {

    // 进入榜单
    Result<RankHomeVO> getRankHome();

    // 更新热歌榜
    void updateHotMusicRank();

    // 更新新歌榜
    void updateNewMusicRank();

    // 更新VIP热歌榜
    void updateVipHotMusicRank();

    // 更新VIP新歌榜
    void updateVipNewMusicRank();
}
