package com.future.music.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@ApiModel("游戏会话请求")
public class GameSessionDTO {
    @NotBlank(message = "会话ID不能为空")
    @ApiModelProperty(value = "游戏会话ID", required = true)
    private String sessionId;
}