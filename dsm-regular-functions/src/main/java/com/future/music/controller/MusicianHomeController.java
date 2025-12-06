package com.future.music.controller;


import com.future.music.entity.vo.MusicDetailVO;
import com.future.music.service.IMusicianHomeService;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Api(tags = "音乐人主页")
@RequestMapping("/musician/home")
public class MusicianHomeController {
    private final IMusicianHomeService musicianHomeService;

    @ApiOperation(value = "分页查询我的音乐作品")
    @GetMapping("/getMyMusic")
    public Result<PageResp<List<MusicDetailVO>>> getMyMusic(@RequestBody PageReq pageReq) {
        return musicianHomeService.getMyMusic(pageReq);
    }


}
