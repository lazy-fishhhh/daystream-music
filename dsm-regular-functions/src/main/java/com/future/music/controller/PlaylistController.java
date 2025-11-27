package com.future.music.controller;

import com.future.music.domain.vo.MusicFilterTagVO;
import com.future.music.domain.vo.PlaylistDetailVO;
import com.future.music.domain.vo.PlaylistVO;
import com.future.music.service.IPlaylistService;
import com.future.music.service.IUserService;
import constants.MusicTagTypeConstants;
import entity.pojo.music.Music;
import entity.pojo.music.MusicTag;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/playlist")
@Api(tags = "歌单")
public class PlaylistController {

    @Resource
    private IPlaylistService playlistService;


    /**
     * 进入歌单详情
     * @param playlistId 歌单ID（从路径参数获取）
     * @return 统一返回格式（成功/失败 + 数据）
     */
    @ApiOperation(value = "进入歌单详情")
    @GetMapping("/detail/{playlistId}")
    public Result<PlaylistDetailVO> getDetail(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @PathVariable Long playlistId) {
        return playlistService.getPlaylistDetail(playlistId, pageNum, pageSize);
    }

    /**
     * 根据标签筛选歌单
     * @param tags 标签ID列表（从请求参数获取）
     * @return 统一返回格式（成功/失败 + 数据）
     */
    @ApiOperation(value = "根据标签筛选歌单")
    @GetMapping("/by-tags")
    public Result<PageResp<PlaylistVO>> filterPlaylistByTagIds(@RequestParam("tags") List<Long> tags,
                                                               @RequestParam(defaultValue = "1") int pageNum,
                                                               @RequestParam(defaultValue = "10") int pageSize){
        return  playlistService.filterPlaylistByTagIds(tags, pageNum, pageSize);
    }

    /**
     * 获取所有可选筛选标签（流派、心情、场景、乐器）
     * @return 分类后的筛选标签列表
     */
    @ApiOperation(value = "获取所有可选筛选标签")
    @GetMapping("/all-tags")
    public Result<MusicFilterTagVO> getAllFilterTags() {
        return playlistService.getAllFilterTags();
    }

}
