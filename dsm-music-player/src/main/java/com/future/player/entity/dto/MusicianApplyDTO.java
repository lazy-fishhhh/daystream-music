package com.future.player.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
@Data
public class MusicianApplyDTO {
   /**
    * 艺名
    */
   @NotBlank(message = "艺名不能为空")
    private String stageName;
   /**
    * 简介
    */
   private String introduction;
   /**
    * 艺人头像
    */
   @NotBlank(message = "艺人头像不能为空")
    private String avatar;
    /**
     * 歌手标签
     */
    @NotNull(message = "歌手标签不能为空")
        private List<Integer> tags;
}
