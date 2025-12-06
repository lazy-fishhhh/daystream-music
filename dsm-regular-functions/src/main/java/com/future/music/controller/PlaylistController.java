package com.future.music.controller;

import com.future.music.domain.vo.MusicFilterTagVO;
import com.future.music.domain.vo.PlaylistDetailVO;
import com.future.music.domain.vo.PlaylistVO;
import com.future.music.service.IPlaylistService;
import com.future.music.service.IUserService;
import entity.dto.PlayListDTO;
import com.future.music.entity.dto.PlaylistFilterDTO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/playlist")
@Api(tags = "歌单")
@RequiredArgsConstructor
public class PlaylistController {
    private final IUserService userService;

    @Resource
    private IPlaylistService playlistService;



    @ApiOperation(value = "创建歌单", notes = "不需要传id")
    @PostMapping("/api/play-list/create-play-list")
    public Result<Void> createPlayList(@Validated @RequestBody PlayListDTO playListDTO) {
        userService.createPlayList(playListDTO);
        return Result.ok();
    }


    @ApiOperation("修改歌单")
    @PostMapping("/playList/update-playList")
    public Result<Void> updatePlayList(@Validated @RequestBody PlayListDTO playList) {
        // 若@Validated校验失败，会自动抛出MethodArgumentNotValidException异常
        userService.updatePlayList(playList);
        return Result.ok();
    }

    @ApiOperation("删除歌单")
    @GetMapping("/playList/delete-playList")
    public Result<Void> deletePlayList(long playListId) {
        userService.deletePlayList(playListId);
        return Result.ok();
    }

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
     * @param filterDTO 歌单筛选请求参数（从请求体获取）
     * @return 统一返回格式（成功/失败 + 数据）
     */
    @ApiOperation(value = "根据标签、时间、收藏量筛选歌单")
    @PostMapping("/by-tags")
    public Result<PageResp<PlaylistVO>> filterPlaylistByTagIds(
                    @Validated @RequestBody PlaylistFilterDTO filterDTO){
        return playlistService.filterPlaylistByTagIds(filterDTO);
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

    @ApiOperation(value = "分页查询我的歌单(按创建时间降序)")
    @GetMapping("/query-my-playlist-by-time")
    public Result<PageResp<List<PlaylistVO>>> queryMyPlaylistByCreateTime(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return playlistService.queryMyPlaylistByCreateTime(pageNum, pageSize);
    }


    @ApiOperation(value = "分页查询我的歌单(按收藏量降序)")
    @GetMapping("/query-my-playlist-by-collect")
    public Result<PageResp<List<PlaylistVO>>> queryMyPlaylistByCollect(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return playlistService.queryMyPlaylistByCollect(pageNum, pageSize);
    }



}

