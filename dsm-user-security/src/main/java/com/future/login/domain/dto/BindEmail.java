package com.future.login.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BindEmail {
    /**
     * 邮箱账号
     */
    @NotBlank(message = "邮箱账号不能为空")
    private String email;
    /**
     * 邮箱密码
     */
    @NotBlank(message = "邮箱密码不能为空")
    private String password;
    /**
     * openId
     */
    @NotBlank(message = "openId不能为空")
    private String openId;
    /**
     * 是否使用之前的姓名 头像
     */
    @NotNull(message = "请选择是否使用之前的姓名 头像")
    private Boolean useOldInfo;
}
