package com.future.player.entity.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class AlbumDTO {
    private Long id;
    
    @NotBlank(message = "专辑名称不能为空")
    private String albumName;

    @NotNull(message = "作者id不能为空")
    private Long musicianId;
    

    private String coverUrl;
    @NotNull(message = "封面文件不能为空")
    /**
     * 封面文件 用于上传文件
     */
    private MultipartFile coverFile;
    
    private LocalDateTime releaseDate; // 发行日期
    
    private String introduction; // 简介
    
    private Integer status; // 状态（0-下架，1-上架）
}