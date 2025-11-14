package com.future.login.domain.vo;

import lombok.Data;

@Data
public class CaptchaVO {
    /**
     * 验证码唯一标识
     */
    private String captchaUUId;
    /**
     * 验证码内容
     */
    private String content;
    /**
     *base64图片
     */
    private String captchaBase64Image;

    /**
     *  过期时间（秒)
     */

    private Long expireSeconds;
}
