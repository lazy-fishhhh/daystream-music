package com.future.login.domain.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginEmailDTO {
    /**
     * 登录邮箱
     */
    @NotNull(message = "登录邮箱不能为空")
    private String email;
    /**
     * 登录密码
     */
    @NotNull(message = "登录密码不能为空")
    private String password;
    /**
     * 验证码
     */
    @NotNull(message = "验证码不能为空")
    private String captcha;
}
