package com.future.music.controller;

import com.future.music.domain.vo.RankHomeVO;
import com.future.music.service.IRankService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "榜单接口")
@RequestMapping("/rank")
public class RankController {

    @Resource
    private IRankService rankService;

    // 进入榜单
    @ApiOperation("进入榜单主页")
    @GetMapping("/home")
    public Result<RankHomeVO> getRankHome() {
        return rankService.getRankHome();
    }


}
