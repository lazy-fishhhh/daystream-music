package com.future.music.domain.dto;

import annotation.IsZeroOrOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.List;

/**
 * MV相关的DTO
 */
@Data
public class MusicMvDTO {

    /**
     * MV名称
     */
    private String mvName;

    /**
     * 关联音乐ID
     */
    private Long musicId;

    /**
     * MV时长（秒）
     */
    private Integer duration;

    /**
     * MV封面URL
     */
    private String coverUrl;

//    /**
//     * MV播放地址
//     */
//    private String mvUrl;

    /**
     * MV发布时间
     */
    private LocalDateTime releaseTime;

    /**
     * 是否VIP专属（0-否，1-是）
     */
    @IsZeroOrOne
    private Integer isVip;

    /**
     * 状态（0-待审核，1-上架，2-下架）
     */
    @Min(value = 0, message = "MV状态值只能为0（未发布）、1（已发布）、2（已下架）")
    @Max(value = 2, message = "MV状态值只能为0（未发布）、1（已发布）、2（已下架）")
    private Integer status;

    /**
     * MV多画质列表
     */
    private List<MvQualityDTO> qualityList;

    /**
     * MV多画质DTO
     */
    @Data
    public static class MvQualityDTO {
        /**
         * 画质类型（1-标清480P，2-高清720P，3-超清1080P，4-4K）
         */
        @Min(value = 1, message = "画质类型只能为1（标清480P）、2（高清720P）、3（超清1080P）、4（4K）")
        @Max(value = 4, message = "画质类型只能为1（标清480P）、2（高清720P）、3（超清1080P）、4（4K）")
        private Integer qualityType;

        /**
         * 对应画质的MV播放地址
         */
        private String mvUrl;

        /**
         * 文件大小（字节）
         */
        private Long fileSize;

        /**
         * 是否仅VIP可播放（0-否，1-是）
         */
        @IsZeroOrOne
        private Integer isVipLimit;
    }
}
