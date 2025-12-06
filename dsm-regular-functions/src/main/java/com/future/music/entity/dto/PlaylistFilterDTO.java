package com.future.music.entity.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 歌单筛选请求DTO
 * 封装根据标签筛选歌单的所有参数
 */
@Data
public class PlaylistFilterDTO {
    /**
     * 标签ID列表
     */
    @NotNull(message = "标签ID列表不能为空")
    private List<Long> tags;
    
    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小，默认10
     */
    private Integer pageSize = 10;
    
    /**
     * 创建开始时间（可选）
     * 格式：ISO 8601 (yyyy-MM-dd'T'HH:mm:ss'Z') 或 yyyy-MM-dd
     */
    private String startTime;
    
    /**
     * 创建结束时间（可选）
     * 格式：ISO 8601 (yyyy-MM-dd'T'HH:mm:ss'Z') 或 yyyy-MM-dd
     */
    private String endTime;
    
    /**
     * 最小收藏量（可选）
     */
    private Integer minCollect;
    
    /**
     * 最大收藏量（可选）
     */
    private Integer maxCollect;
}