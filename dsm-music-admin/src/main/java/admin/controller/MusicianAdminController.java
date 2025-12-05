package admin.controller;

import admin.domain.dto.MusicianDTO;
import admin.domain.dto.MusiciansDTO;
import admin.domain.vo.MusicianVO;
import admin.service.AdminMusicianService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin")
@Api(tags = "音乐人认证管理")
public class MusicianAdminController {
    @Autowired
    private AdminMusicianService adminMusicianService;
    //@PreAuthorize("hasRole('管理员')")
    @ApiOperation(value = "获取音乐人审核列表")
    @PostMapping("/getMusicianList")
    public Result<PageResp<MusicianVO>> getMusicianList(@Valid @RequestBody MusiciansDTO musicianDTO) {
        return adminMusicianService.getMusicianList(musicianDTO);
    }
    //@PreAuthorize("hasRole('管理员')")
    @ApiOperation(value = "音乐人认证审核")
    @PostMapping("/applyMusician")
    public Result applyMusician(@Valid @RequestBody MusicianDTO musicianDTO) {
        return adminMusicianService.applyMusician(musicianDTO);
    }
}
