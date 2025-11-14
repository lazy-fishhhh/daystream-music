package com.future.login.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CaptchaDTO {
    /**
     * 验证码
     */
    @NotNull(message = "验证码不能为空")
    private String captcha;
    /**
     * 验证码唯一标识 uuid
     */
    @NotNull(message = "验证码唯一标识uuid不能为空")
    private String captchaUUId;
}

