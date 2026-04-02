package com.future.player.controller;

import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import com.future.player.entity.dto.DisableAccountDTO;
import com.future.player.entity.dto.MusicianApplyDTO;
import com.future.player.entity.dto.MusicianListDTO;
import com.future.player.entity.dto.UpdateMusicianDTO;
import com.future.player.entity.vo.MusicianDetailVO;
import com.future.player.entity.vo.MusicianInfoVO;
import com.future.player.service.MusicianApplyService;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Api(tags = "音乐人管理")
@RestController
public class MusicianApplyController {
    @Autowired
    private MusicianApplyService musicianApplyService;
    @ApiOperation(value="用户申请入驻音乐人")
    @PostMapping("/applyMusician")
    public Result<String> applyMusician(@Valid @RequestBody MusicianApplyDTO applyDTO)
    {
        return musicianApplyService.applyMusician(applyDTO);
    }
    @ApiOperation(value="判断是否是音乐人")
    @PostMapping("/isMusician")
    public Result<Boolean> isMusician()
    {
        return musicianApplyService.isMusician();
    }
    @ApiOperation(value = "撤销未审核的入驻申请")
    @DeleteMapping("/cancelApply")
    public Result<String> cancelApply()
    {
        return musicianApplyService.cancelApply();
    }
    @PreAuthorize("hasRole('音乐人')")
    @ApiOperation(value = "注销音乐人" ,notes = "注销音乐人后变成普通用户")
    @PostMapping("/deleteMusician")
    public Result<String> deleteMusician(@Valid @RequestBody DisableAccountDTO disableAccountDTO)
    {
        return musicianApplyService.deleteMusician(disableAccountDTO);
    }
    @PreAuthorize("hasAnyRole('音乐人','管理员')")
    @ApiOperation(value = "获取当前音乐人信息")
    @GetMapping("/getMuscianInfo")
    public Result<MusicianInfoVO> getMuscianInfo()
    {
        return musicianApplyService.getMuscianInfo();
    }
    @ApiOperation(value = "获取所有音乐人")
    @PostMapping("/getAllMusician")
    public Result<PageResp<MusicianInfoVO>> getAllMusician(@Valid @RequestBody MusicianListDTO musicianListDTO)
    {
        return musicianApplyService.getAllMusician(musicianListDTO);
    }
    @ApiOperation(value = "更改音乐人的信息")
    @PreAuthorize("hasAnyRole('音乐人','管理员')")
    @PostMapping("/updateMusicianInfo")
    public Result<String> updateMusicianInfo(@Valid @RequestBody UpdateMusicianDTO updateMusicianDTO)
    {
        return musicianApplyService.updateMusicianInfo(updateMusicianDTO);
    }
    @ApiOperation(value="获取音乐人的个人信息(跳转)")
    @GetMapping("getMusicianDetail")
    public Result<MusicianDetailVO> getMusicianDetail(@RequestParam @NotBlank(message = "音乐人ID不能为空") String musicianId)
    {
        return musicianApplyService.getMusicianDetail(musicianId);
    }


}
