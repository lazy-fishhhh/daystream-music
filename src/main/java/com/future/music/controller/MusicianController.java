package com.future.music.controller;


import com.future.music.entity.dto.PlayListDTO;
import com.future.music.entity.pojo.util.Result;
import com.future.music.entity.pojo.music.PlayList;
import com.future.music.service.IMusicianService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@RequiredArgsConstructor
@Controller
public class MusicianController {
    public final IMusicianService musicianService;

    @ApiOperation("创建歌单")
    @PostMapping("/musician/createPlayList")
    public Result<Void> createMusician(PlayListDTO playListDTO) {
            musicianService.createPlayList(playListDTO);
        return Result.ok();
    }


    @ApiOperation("修改歌单")
    @PostMapping("/musician/updatePlayList")
    public Result<Void> updateMusician(PlayListDTO playList) {

        musicianService.updatePlayList(playList);
        return Result.ok();
    }

    @ApiOperation("获取所有歌单")
    @GetMapping("/musician/getAllPlayList")
    public Result<PlayList> getMusician() {
        PlayList playList = musicianService.getPlayList();
        return Result.ok(playList);
    }


}
