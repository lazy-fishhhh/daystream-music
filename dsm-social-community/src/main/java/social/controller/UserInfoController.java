package social.controller;

import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import social.service.UserInfoService;

@RestController
@Api(tags = "用户信息")
@RequestMapping("/user")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;
    @ApiOperation(value="获取当前登录用户的详细信息")
    @GetMapping("/getUserInfo")
    public Result getUserInfo()
    {
        return userInfoService.getUserInfo();
    }
    @ApiOperation(value="获取指定用户的信息")
    @GetMapping("/getOtherUserInfo")
    public Result getOtherUserInfo( @RequestParam(value = "userId",required = true) Long userId)
        {
            return userInfoService.getOtherUserInfo(userId);
        }
}
