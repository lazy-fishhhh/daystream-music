package admin.controller;

import admin.domain.dto.MusicianListDTO;
import admin.domain.vo.MusicianListVO;
import admin.domain.vo.MusicianVO;
import admin.service.AdminMusicianService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/adminMusician")
@Api(tags = "音乐人列表管理")
public class MusicianListController {
    @Autowired
    private AdminMusicianService adminMusicianService;
    @PostMapping("/list")
    @ApiOperation(value = "音乐人列表")
    public Result<PageResp<MusicianListVO>> listMusician(@Valid @RequestBody MusicianListDTO musicianListDTO){
        return adminMusicianService.getMusician(musicianListDTO);
    }
}
