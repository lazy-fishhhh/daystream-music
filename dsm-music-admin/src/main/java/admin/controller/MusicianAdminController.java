package admin.controller;

import admin.domain.dto.MusicianDTO;
import admin.domain.vo.MusicianVO;
import admin.service.AdminMusicianService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @ApiOperation(value = "获取音乐人审核列表")
    @PostMapping("/getMusicianList")
    public Result<PageResp<MusicianVO>> getMusicianList(@Valid @RequestBody MusicianDTO musicianDTO) {
        return adminMusicianService.getMusicianList(musicianDTO);
    }
    @ApiOperation(value = "音乐人认证审核")
    public Result applyMusician(MusicianDTO dto) {
        return null;
    }
}
