package com.future.player.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class AlbumDTO {
    private Long id; // 修改时需要
    
    @NotBlank(message = "专辑名称不能为空")
    private String albumName;
    
    @NotNull(message = "作者id不能为空")
    private Long musicianId;
    
    @NotBlank(message = "专辑封面url不能为空")
    private String coverUrl;
    
    private LocalDateTime releaseDate; // 发行日期
    
    private String introduction; // 简介
    
    private Integer status; // 状态（0-下架，1-上架）
}