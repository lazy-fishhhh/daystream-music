package com.future.login.controller;

import com.future.login.domain.dto.LoginEmailDTO;
import com.future.login.domain.vo.CaptchaVO;
import com.future.login.service.CaptchaService;
import com.future.login.service.LoginService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

@Api(tags = "登录管理")
@RestController
@RequestMapping("/login")
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Autowired
    private CaptchaService captchaService;
    @ApiOperation("通过邮箱密码登录")
    @PostMapping("/loginByemail")
    public Result loginByEmail(@Valid @RequestBody LoginEmailDTO loginEmailDTO)
    {
        return loginService.LoginByEmail(loginEmailDTO);
    }
    @ApiOperation("获取图形验证码")
    @GetMapping("/getCaptcha")
    public Result<CaptchaVO> Captcha()
    {
        return captchaService.creatCaptcha();
    }
    @ApiOperation("退出登录")
    @GetMapping("/loginout")
    public Result loginOut()
    {
        return loginService.LoginOut();
    }
}
