package com.future.login.controller;

import com.future.login.domain.dto.BindEmail;
import com.future.login.domain.dto.FogetDTO;
import com.future.login.domain.dto.LoginEmailDTO;
import com.future.login.domain.vo.CaptchaVO;
import com.future.login.service.CaptchaService;
import com.future.login.service.LoginService;
import com.future.login.service.QQAuthService;
import com.future.login.service.impl.QQAuthServiceImpl;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "登录管理")
@RestController
@RequestMapping("/login")
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private QQAuthServiceImpl qqAuthService;
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
    @ApiOperation("忘记密码")
    @PostMapping("/fogetPassword")
    public Result fogetPassword(@Valid @RequestBody FogetDTO fogetDTO)
    {
        return loginService.fogetPassword(fogetDTO);
    }
    @ApiOperation("获取qq登录授权url")
    @GetMapping("/url")
    public Result<String> getAuthorizationUrl()
    {
        return qqAuthService.getAuthorizationUrl();
    }
    @ApiOperation("QQ登录回调接口")
    @GetMapping("/callback")
    public void qqCallback(@RequestParam("code") String code,
                                     @RequestParam(value = "state", required = false) String state,
                                HttpServletResponse response) {
        qqAuthService.handleCallback(code, state,response);
    }
    /*@ApiOperation("绑定之前创建的邮箱账号")
    @PostMapping("/bindEmail")
    public Result bindEmail(@Valid @RequestBody BindEmail bindEmail)
    {
        return loginService.bindEmail(bindEmail);
    }*/

    @ApiOperation("使用临时code获取qq登录的token")
    @GetMapping("/getToken")
    public Result<HashMap<String,String>> getToken(@RequestParam(value = "tempCode",required = true)String tempCode)
    {
        return qqAuthService.getToken(tempCode);
    }
}
