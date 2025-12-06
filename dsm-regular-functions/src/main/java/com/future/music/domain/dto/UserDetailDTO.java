package com.future.music.domain.dto;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserDetailDTO {



    private String username;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 用户性别
     */
    @ApiModelProperty(value = "0-未知,1-男,2-女")
    private Integer gender;
    /**
     * 用户简介
     */
    private String introduction;


    /**
     * 用户手机号
     */
    private String phone;


}
