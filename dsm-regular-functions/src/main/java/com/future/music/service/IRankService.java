package com.future.music.service;

import com.future.music.domain.vo.RankHomeVO;
import entity.pojo.util.Result;

public interface IRankService {

    // 进入榜单
    Result<RankHomeVO> getRankHome();

}
