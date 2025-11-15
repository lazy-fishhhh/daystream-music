package com.future.login.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RejesterDTO {
    /**
     *用户姓名
     */
    @NotNull(message = "姓名不能为空")
    private String username;
    /**
     * 邮箱
     */
    @NotNull(message = "邮箱不能为空")
    private String email;
    /**
     * 密码
     */
    @NotNull(message = "密码不能为空")
    private String password;
    /**
     * 再次确认的密码
     */
    @NotNull(message = "确认密码不能为空")
    private String newPassword;
    /**
     * 邮箱验证码
     */
    @NotNull(message = "邮箱验证码不能为空")
    private String captcha;

}
