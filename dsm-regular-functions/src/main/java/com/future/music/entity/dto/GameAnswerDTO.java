package com.future.music.entity.dto;

import io.swagger.annotations.ApiModel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("答题请求")
public class GameAnswerDTO {
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotNull(message = "歌曲ID不能为空")
    private Long musicId;

    @NotBlank(message = "答案不能为空")
    private String answer;
}