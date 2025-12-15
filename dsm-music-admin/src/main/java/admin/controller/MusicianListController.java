package admin.controller;

import admin.domain.dto.*;
import admin.domain.vo.*;
import admin.service.AdminMusicianService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    public Result<PageResp<MusicianListVO>> listMusician(@Valid @RequestBody MusicianListDTO musicianListDTO) {
        return adminMusicianService.getMusician(musicianListDTO);
    }

    @GetMapping("/banMusician")
    @ApiOperation(value = "封禁/解封音乐人")
    public Result<BanUserStatusVO> banMusician(@NotBlank(message = "音乐人id不能为空") @RequestParam(value = "musicianId", required = true) String musicianId) {
        return adminMusicianService.banMusician(musicianId);
    }

    @GetMapping("/deleteMusician")
    @ApiOperation(value = "删除音乐人")
    public Result<String> deleteMusician(@NotBlank(message = "音乐人id不能为空") @RequestParam(value = "musicianId", required = true) String musicianId) {
        return adminMusicianService.deleteMusician(musicianId);
    }

    @PostMapping("/deleteBatch")
    @ApiOperation(value = "批量删除音乐人")
    public Result<String> deleteBatch(@Valid @RequestBody DeletedMusicianDTO deletedMusicianDTO) {
        return adminMusicianService.deleteBatch(deletedMusicianDTO);
    }

    @ApiOperation(value = "查看音乐人详情")
    @GetMapping("detailMusician")
    public Result<DetailMusicianVO> DetailUser(@NotBlank(message = "音乐人id不能为空") @RequestParam(name = "musicianId", required = true) String musicianId) {
        return adminMusicianService.getDteailMusica(musicianId);
    }

    @ApiOperation(value = "修改音乐人信息")
    @PostMapping("updateMusician")
    public Result<String> updateMusician(@Valid @RequestBody UpdateMusicianDTO updateMusicianDTO) {
        return adminMusicianService.updateMusicianInfo(updateMusicianDTO);
    }
}


