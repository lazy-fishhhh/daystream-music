package com.example.music.controller;

import com.future.music.entity.dto.MusicDTO;
import com.future.music.entity.pojo.util.Result;
import com.future.music.service.IMusicService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Api(tags = "音乐")
@RequestMapping("/music")
public class MusicController {

    @Autowired
    private IMusicService iMusicService;

    @ApiOperation(value = "上传音乐")
    @PostMapping("/saveMusic")
    public Result saveMusic(@RequestPart("musicDTO") MusicDTO musicDTO,
                            @RequestPart("lyric") MultipartFile lyric,
                            @RequestPart("cover") MultipartFile cover,
                            @RequestPart("audio") MultipartFile audio) {
        return iMusicService.saveMusic(musicDTO, lyric, cover, audio);
    }

    @ApiOperation(value = "删除音乐")
    @PostMapping("/removeMusic")
    public Result saveMusic(@RequestParam Long musicId) {
        return iMusicService.removeMusic(musicId);
    }

}
