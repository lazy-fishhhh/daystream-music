package com.future.login.controller;

import com.future.login.domain.dto.LoginEmailDTO;
import com.future.login.service.LoginService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Api(tags = "登录管理")
@RestController
@RequestMapping("/login")
public class LoginController {
    @Autowired
    private LoginService loginService;

    @ApiOperation("通过邮箱密码登录")
    @PostMapping("/loginByemail")
    public Result loginByEmail(@Valid @RequestBody LoginEmailDTO loginEmailDTO)
    {
        return loginService.LoginByEmail(loginEmailDTO);
    }
}
