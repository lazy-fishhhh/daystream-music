package com.future.music.controller;

import com.future.music.domain.vo.*;
import com.future.music.service.IPlaylistService;
import com.future.music.service.IUserService;
import entity.dto.PlayListDTO;
import com.future.music.entity.dto.PlaylistFilterDTO;
import entity.pojo.music.Music;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    @PostMapping(value = "/api/play-list/create-play-list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PlaylistVO> createPlayList(@Validated @ModelAttribute PlayListDTO playListDTO) {

        return userService.createPlayList(playListDTO);
    }


    @ApiOperation("修改歌单")
    @PostMapping(value = "/playList/update-playList", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> updatePlayList(@Validated @ModelAttribute PlayListDTO playList) {
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
                                              @PathVariable String playlistId) {
        Long id = Long.valueOf(playlistId);
        return playlistService.getPlaylistDetail(id, pageNum, pageSize);
    }

    /**
     * 获取歌单收藏者分页列表
     * @param playlistId 歌单ID（从路径参数获取）
     * @param pageNum 分页页码（默认1）
     * @param pageSize 分页大小（默认20，最大50）
     * @return 收藏者分页列表（包含用户ID、用户名、头像等）
     */
    @ApiOperation(value = "获取歌单收藏者分页列表")
    @GetMapping("/collectors/{playlistId}")
    public Result<List<CollectorVO>> getPlaylistCollectors(@PathVariable String playlistId,
                                                            @RequestParam(defaultValue = "1") int pageNum,
                                                            @RequestParam(defaultValue = "12") int pageSize) {
        return playlistService.getPlaylistCollectors(playlistId, pageNum, pageSize);
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


    @ApiOperation(value = "将歌曲添加到指定歌单")
    @PostMapping("/add-song-to-playlist")
    public Result<String> addSongToPlaylist(@RequestParam Long playlistId, @RequestParam Long songId) {
        return playlistService.addSongToPlaylist(playlistId, songId);
    }

    @ApiOperation(value = "批量将歌曲添加到指定歌单")
    @PostMapping("/batch-add-song-to-playlist")
    public Result<String> batchAddSongToPlaylist(@RequestParam Long playlistId, @RequestParam List<Long> songIds) {
        return playlistService.batchAddSongToPlaylist(playlistId, songIds);
    }

    @ApiOperation(value = "将歌曲从指定歌单中移除")
    @PostMapping("/remove-song-from-playlist")
    public Result<String> removeSongFromPlaylist(@RequestParam Long playlistId, @RequestParam Long songId) {
        return playlistService.removeSongFromPlaylist(playlistId, songId);
    }

    @ApiOperation(value = "批量将歌曲从指定歌单中移除")
    @PostMapping("/batch-remove-song-from-playlist")
    public Result<String> batchRemoveSongFromPlaylist(@RequestParam Long playlistId, @RequestParam List<Long> songIds) {
        return playlistService.batchRemoveSongFromPlaylist(playlistId, songIds);
    }

    @ApiOperation(value = "分页查询指定歌单的歌曲")
    @GetMapping("/query-songs-by-playlist")
    public Result<PageResp<MusicVO>> querySongsByPlaylist(@RequestParam Long playlistId,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        return playlistService.querySongsByPlaylist(playlistId, pageNum, pageSize);
    }

}

