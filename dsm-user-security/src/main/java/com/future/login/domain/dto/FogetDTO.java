package com.future.login.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;



@Data
public class FogetDTO {
    /**
     * 邮箱
     */
    @NotNull(message = "邮箱不能为空")
    private String email;
    /**
     * 邮箱验证码
     */
    @NotNull(message = "邮箱验证码不能为空")
    private String captcha;
    /**
     * 新密码
     */
    @NotNull(message = "新密码不能为空")
    private String password;
    /**
     * 新密码确认
     */
    @NotNull(message = "新密码验证不能为空")
    private String newPassword;
}

