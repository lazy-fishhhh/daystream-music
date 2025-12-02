package social.controller;

import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import social.domain.dto.MusicianApplyDTO;
import social.service.UserMusicianService;

@RestController
@Api(tags = "音乐人认证")
@RequestMapping("/user")
public class UserMusicianController {
    @Autowired
    private UserMusicianService userMusicianService;
    @ApiOperation(value="首页用户信息展示")
    @GetMapping("/getUserInfo")
    public Result getUserInfo()
    {
        return userMusicianService.getUserInfo();
    }
        @ApiOperation(value="用户申请入驻音乐人")
        @PostMapping("/applyMusician")
        public Result applyMusician(@Valid @RequestBody MusicianApplyDTO applyDTO)
        {
            return userMusicianService.applyMusician(applyDTO);
        }
        @ApiOperation(value="判断是否是音乐人")
        @PostMapping("/isMusician")
        public Result cancelMusician()
        {
            return userMusicianService.isMusician();
        }



}
