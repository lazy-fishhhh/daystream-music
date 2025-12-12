package entity.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * MV详情VO
 */
@Data
public class MusicMvVO {

    /**
     * MV主键ID
     */
    private Long id;

    /**
     * MV名称
     */
    private String mvName;

    /**
     * 关联音乐ID
     */
    private Long musicId;

    /**
     * 音乐名称
     */
    private String musicName;

    /**
     * MV时长（秒）
     */
    private Integer duration;

    /**
     * MV封面URL（首帧截图）
     */
    private String coverUrl;

    /**
     * MV多画质列表
     */
    private List<MvQualityVO> qualityList;

    /**
     * MV发布时间
     */
    private Date releaseTime;

    /**
     * 播放量
     */
    private Long playCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 是否VIP专属（0-否，1-是）
     */
    private Integer isVip;

    /**
     * 用户是否已点赞（0-否，1-是）
     */
    private Integer isLiked;

    /**
     * 用户是否已收藏（0-否，1-是）
     */
    private Integer isCollected;

    /**
     * 状态（0-待审核，1-上架，2-下架）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * MV画质VO类
     */
    @Data
    public static class MvQualityVO {
        /**
         * 画质类型（1-标清480P，2-高清720P，3-超清1080P，4-4K）
         */
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
        private Integer isVipLimit;
    }
}
