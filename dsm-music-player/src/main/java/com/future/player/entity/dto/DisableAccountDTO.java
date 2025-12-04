package com.future.player.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 注销音乐人
 */
@Data
public class DisableAccountDTO {
    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    private String email;
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    /**
     * 邮箱验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captcha;
}

