package com.future.music.controller;


import com.future.login.util.SecurityUtil;
import com.future.music.domain.dto.MusicPlayReportDTO;
import com.future.music.domain.vo.*;
import com.future.music.service.IMusicService;
import com.future.music.service.IPlaylistService;
import com.future.player.mapper.AlbumMapper;
import entity.dto.MusicDTO;
import entity.dto.MusicFilterDTO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.MusicAlbumVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "音乐")
@RequestMapping("/music")
public class MusicController {

    @Autowired
    private IMusicService iMusicService;

    @Autowired
    private AlbumMapper albumMapper;

    @Resource
    private IPlaylistService playlistService;

    @ApiOperation(value = "获取所有可选筛选标签")
    @GetMapping("/all-tags")
    public Result<MusicFilterTagVO> getAllFilterTags() {
        return playlistService.getAllFilterTags();
    }

    @ApiOperation(value = "上传音乐")
    @PostMapping("/upload")
    public Result<Void> saveMusic(@RequestPart() MusicDTO musicDTO,
                            @RequestPart("lyric") MultipartFile lyric,
                            @RequestPart("cover") MultipartFile cover,
                            @RequestPart(value = "audio_1", required = false) MultipartFile audio1,  // 音质1：标准
                            @RequestPart(value = "audio_2", required = false) MultipartFile audio2,  // 音质2：高清
                            @RequestPart(value = "audio_3", required = false) MultipartFile audio3,  // 音质3：无损
                            @RequestPart(value = "audio_4", required = false) MultipartFile audio4   // 音质4：空间
                            ) {
        return iMusicService.saveMusic(musicDTO, lyric, cover, audio1, audio2, audio3, audio4);
    }

    @ApiOperation(value = "删除音乐")
    @DeleteMapping("/{musicId}") // 路径参数定位资源，语义更清晰
    public Result<Void> removeMusic(@PathVariable Long musicId) { // 配合路径参数，而非@RequestParam
        return iMusicService.removeMusic(musicId);
    }

    @ApiOperation(value = "回显音乐")
    @GetMapping("/echo/{musicId}")
    public Result<EchoMusicVO> echoMusic(@PathVariable Long musicId) {
        return iMusicService.echoMusic(musicId);
    }

    @ApiOperation(value = "修改音乐")
    @PostMapping("/update/{musicId}") // POST支持文件+JSON混合参数
    public Result<Void> updateMusic(
            @PathVariable("musicId") Long musicId, // 路径参数，必选（确保修改目标明确）
            @RequestPart("musicDTO") MusicDTO musicDTO, // JSON参数（需用@RequestPart接收混合参数）
            @RequestPart(value = "lyric", required = false) MultipartFile lyric, // 可选文件
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart(value = "audioMap", required = false) Map<Integer, MultipartFile> audioMap) {
        // 直接调用业务层方法，返回结果（业务层已处理校验和异常）
        return iMusicService.updateMusic(musicId, musicDTO, lyric, cover, audioMap);
    }

    @ApiOperation(value = "查询单个音乐信息")
    @GetMapping("/one/{musicId}")
    public Result<MusicDetailVO> getOneMusic(@PathVariable Long musicId) {
        return iMusicService.getOneMusic(musicId);
    }

    @ApiOperation(value = "下载音乐")
    @GetMapping("/downloadMusic")
    public Result<Void> downloadMusic(@RequestParam Long musicId,
                            @RequestParam Integer quality,
                            HttpServletResponse response) {
        try {
            iMusicService.downloadMusic(musicId, quality, response);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("下载失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "播放音乐（获取播放链接）")
    @PostMapping("/play")
    public Result<MusicPlayVO> playMusic(@RequestParam Long musicId,
                                         @RequestParam Integer quality) {
        return iMusicService.playMusic(musicId, quality);
    }

    @ApiOperation(value = "上报播放状态（统计播放量）")
    @PostMapping("/play/report")
    public Result<Void> reportPlayStatus(@RequestBody MusicPlayReportDTO dto) {
        return iMusicService.reportPlayStatus(dto);
    }

    /**
     * 根据标签筛选音乐
     * @param filterDTO 音乐筛选请求参数（从请求体获取）
     * @return 统一返回格式（成功/失败 + 数据）
     */
    @ApiOperation(value = "根据标签筛选音乐")
    @PostMapping("/by-tags")
    public Result<PageResp<MusicVO>> filterMusicByTagIds(
            @Validated @RequestBody MusicFilterDTO filterDTO){
        return iMusicService.filterMusicByTags(filterDTO);
    }

    @ApiOperation(value = "上传音乐时查询自己所有专辑")
    @GetMapping("/albums")
    public Result<List<MusicAlbumVO>> getAllAlbums() {
        if (SecurityUtil.getCurrentUser() == null) {
            return Result.fail("用户未登录");
        }
        return Result.ok(albumMapper.selectAllAlbumByUserId(SecurityUtil.getCurrentUser().getId()));
    }

}
