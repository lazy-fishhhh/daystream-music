package com.future.player.controller;

import com.future.player.entity.dto.AlbumDTO;
import com.future.player.entity.vo.AlbumVO;
import com.future.player.service.IAlbumService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(value = "专辑管理")
@RestController
public class AlbumController {

    @Autowired
    private IAlbumService albumService;

    @ApiOperation(value = "创建专辑")
    @PostMapping("/api/album/create-album")
    public Result<Void> createAlbum(@Validated @RequestBody AlbumDTO albumDTO) {
        return albumService.createAlbum(albumDTO);
    }


    @ApiOperation(value = "修改专辑")
    @PostMapping("/api/album/update-album")
    public Result<Void> updateAlbum(@Validated @RequestBody AlbumDTO albumDTO) {
        return albumService.updateAlbum(albumDTO);
    }


    @ApiOperation(value = "删除专辑")
    @GetMapping("/api/album/delete-album")
    public Result<Void> deleteAlbum(@RequestParam Long albumId) {
        return albumService.deleteAlbum(albumId);
    }


    @ApiOperation(value = "分页查询我的所有专辑")
    @GetMapping("/api/album/query-my-album")
    public Result<PageResp<AlbumVO>> queryMyAlbum(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return albumService.queryMyAlbum(pageNum, pageSize);
    }

    @ApiOperation(value = "分页查询公开专辑", notes = "先按点赞量降序，再按发布时间降序")
    @GetMapping("/api/album/query-public-album")
    public Result<PageResp<AlbumVO>> queryPublicAlbum(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return albumService.queryPublicAlbum(pageNum, pageSize);
    }


    @ApiOperation(value = "分页查询指定音乐人的公开专辑")
    @GetMapping("/api/album/query-other-album")
    public Result<PageResp<AlbumVO>> queryOtherAlbum(@RequestParam Long userId,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return albumService.queryOtherAlbum(userId, pageNum, pageSize);
    }
    @ApiOperation(value = "根据关键词搜索专辑" ,notes = "这里的key可以是音乐人名，也可以是专辑名")
    @GetMapping("/api/album/search-album")
    public Result<PageResp<AlbumVO>> queryAllAlbum(@RequestParam String key,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return albumService.queryAllAlbum(key,pageNum, pageSize);
    }


    @ApiOperation(value = "获取专辑详细内容")
    @GetMapping("/api/album/get-album-detail")
    public Result<AlbumVO> getAlbumDetail(@RequestParam Long albumId) {
        return albumService.getAlbumDetail(albumId);
    }


}