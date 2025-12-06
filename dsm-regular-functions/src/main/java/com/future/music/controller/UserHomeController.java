package com.future.music.controller;


import com.future.music.domain.dto.UserDetailDTO;
import com.future.music.domain.vo.MusicVO;
import com.future.music.domain.vo.PlaylistVO;
import com.future.music.service.IUserHomeService;
import entity.pojo.music.Album;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import social.domain.vo.UserInfoVO;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/home")
@Api(tags = "普通用户主页")
public class UserHomeController {
    private final IUserHomeService userHomeService;

    @ApiOperation(value = "获取个人信息")
    @GetMapping("/getUserInfo")
    public Result<UserInfoVO> getUserInfo() {
        return userHomeService.getUserInfo();
    }


    @ApiOperation("更新用户信息")
    @PostMapping(value = "/updateUserInfo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> updateUserInfo(UserDetailDTO dto) {
        return userHomeService.updateUserInfo(dto);
    }

    @ApiOperation("查询我喜欢的歌曲")
    @GetMapping("/getMyLikeSongs")
    public Result<PageResp<List<MusicVO>>> getMyLikeSongs(@RequestBody PageReq pageReq) {
        return userHomeService.getMyLikeSongs(pageReq);
    }

    @ApiOperation("查询我收藏的歌单")
    @GetMapping("/getMyCollectSongLists")
    public Result<PageResp<List<PlaylistVO>>>getMyCollectSongLists(@RequestBody PageReq pageReq) {
        return userHomeService.getMyCollectSongLists(pageReq);
    }

    @ApiOperation("查询我收藏的专辑")
    @GetMapping("/getMyCollectAlbums")
    public Result<PageResp<List<Album>>> getMyCollectAlbums(@RequestBody PageReq pageReq) {
        return userHomeService.getMyCollectAlbums(pageReq);
    }


}
