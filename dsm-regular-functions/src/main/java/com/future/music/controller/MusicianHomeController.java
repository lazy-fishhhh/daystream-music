package com.future.music.controller;


import com.future.music.domain.vo.AlbumVO;
import com.future.music.entity.vo.MusicDetailVO;
import com.future.music.service.IMusicianHomeService;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @ApiOperation("获取专辑详细信息")
    @GetMapping("/getAlbumDetail")
    public Result<AlbumVO> getAlbumDetail(@RequestParam Long albumId) {
        return musicianHomeService.getAlbumDetail(albumId);
    }

}
