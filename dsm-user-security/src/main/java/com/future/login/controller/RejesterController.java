package com.future.login.controller;

import com.future.login.domain.dto.RejesterDTO;
import com.future.login.service.EmailService;
import com.future.login.service.RejesterService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Api(tags = "注册管理")
@RequestMapping("/rejester")
public class RejesterController {
    @Autowired
    private EmailService emailService;
    @Autowired
    private RejesterService rejesterService;

    @ApiOperation("发送邮箱验证码")
    @GetMapping("/getEmailCaptcha")
    public Result sendCaptcha(@RequestParam String toEmail)
    {
        try {
            return emailService.sendCaptcha(toEmail);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @ApiOperation("注册用户")
    @PostMapping("/insertUser")
    public Result insertUser(@RequestBody @Valid RejesterDTO rejesterDTO)
    {
        return rejesterService.rejester(rejesterDTO);
    }

}
