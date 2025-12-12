package admin.controller;

import admin.domain.dto.MusicianListDTO;
import admin.domain.vo.MusicianListVO;
import admin.domain.vo.MusicianVO;
import admin.service.AdminMusicianService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/adminMusician")
@Api(tags = "音乐人列表管理")
public class MusicianListController {
    @Autowired
    private AdminMusicianService adminMusicianService;
    @PostMapping("/list")
    @ApiOperation(value = "音乐人列表")
    public Result<PageResp<MusicianListVO>> listMusician(MusicianListDTO musicianListDTO){
        return adminMusicianService.getMusician(musicianListDTO);
    }
}
