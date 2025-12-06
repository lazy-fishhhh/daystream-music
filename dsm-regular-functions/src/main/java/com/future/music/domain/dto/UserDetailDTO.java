package com.future.music.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserDetailDTO {
    private String username;
    /**
     * 用户头像文件
     */
    @ApiModelProperty(value = "头像图片文件")
    private MultipartFile avatarFile; // 仅新增这一行

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "不传")
    private String avatar;

    /**
     * 用户性别
     */
    @ApiModelProperty(value = "0-未知,1-男,2-女")
    private String gender;
    /**
     * 用户简介
     */
    private String introduction;
    /**
     * 用户手机号
     */
    private String phone;
    /**
     * 背景图文件
     */
    @ApiModelProperty(value = "背景图片文件")
    private MultipartFile backgroundImageFile;
    /**
     * 背景图
     */
    @ApiModelProperty(value = "不传")
    private String backgroundImage;
}