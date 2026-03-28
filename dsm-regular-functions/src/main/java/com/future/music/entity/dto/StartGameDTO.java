package com.future.music.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Max;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "开始游戏请求参数", description = "游戏创建时传入的配置信息")
public class StartGameDTO {

    /**
     * 游戏类型id（1:听音猜歌 / 2:歌词接龙 / 3:猜作者）
     */
    @NotNull(message = "游戏类型不能为空")
    @Min(value = 1, message = "游戏类型不符合要求")
    @Max(value = 3, message = "游戏类型不符合要求")
    @ApiModelProperty(value = "游戏类型ID 1-听音猜歌 2-歌词接龙 3-猜作者", required = true, example = "1")
    private Integer gameTypeId;

    /**
     * 难度（简单:1 / 中等:2 / 困难:3）
     */
    @NotNull(message = "游戏难度不能为空")
    @Min(value = 1, message = "游戏难度不符合要求")
    @Max(value = 3, message = "游戏难度不符合要求")
    @ApiModelProperty(value = "难度 1-简单 2-中等 3-困难", required = true, example = "2")
    private Integer difficulty;
}
