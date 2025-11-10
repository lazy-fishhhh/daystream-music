package com.example.music.controller;


import com.example.music.entity.dto.PlayListDTO;
import com.example.music.entity.pojo.Result;
import com.example.music.entity.pojo.music.PlayList;
import com.example.music.service.IMusicianService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@RequiredArgsConstructor
@Controller
public class MusicianController {
    public final IMusicianService musicianService;

    @ApiOperation("创建歌单")
    @GetMapping("/musician/createPlayList")
    public Result<Void> createMusician(PlayListDTO playListDTO) {
            musicianService.createPlayList(playListDTO);
        return Result.ok();
    }




}
