package com.future.music.controller;


import com.future.music.domain.vo.MusicDetailVO;
import com.future.music.service.IMusicService;
import entity.dto.MusicDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.Music;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Api(tags = "音乐")
@RequestMapping("/music")
public class MusicController {

    @Autowired
    private IMusicService iMusicService;

    @ApiOperation(value = "上传音乐")
    @PostMapping
    public Result saveMusic(@RequestPart("musicDTO") MusicDTO musicDTO,
                            @RequestPart("lyric") MultipartFile lyric,
                            @RequestPart("cover") MultipartFile cover,
                            @RequestPart("audioMap") Map<Integer, MultipartFile> audioMap) {
        return iMusicService.saveMusic(musicDTO, lyric, cover, audioMap);
    }

    @ApiOperation(value = "删除音乐")
    @DeleteMapping("/{musicId}") // 路径参数定位资源，语义更清晰
    public Result removeMusic(@PathVariable Long musicId) { // 配合路径参数，而非@RequestParam
        return iMusicService.removeMusic(musicId);
    }

    @ApiOperation(value = "回显音乐")
    @GetMapping("/echo/{musicId}")
    public Result<MusicDetailVO> echoMusic(@PathVariable Long musicId) {
        return iMusicService.echoMusic(musicId);
    }

    @ApiOperation(value = "修改音乐")
    @PostMapping("/update/{musicId}") // POST支持文件+JSON混合参数
    public Result updateMusic(
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
    public Result downloadMusic(@RequestParam Long musicId,
                            @RequestParam Integer quality,
                            HttpServletResponse response) {
        try {
            iMusicService.downloadMusic(musicId, quality, response);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("下载失败：" + e.getMessage());
        }
    }
}
