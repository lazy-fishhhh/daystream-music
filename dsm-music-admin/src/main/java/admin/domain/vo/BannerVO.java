package admin.domain.vo;

import admin.domain.enums.BannerActionTypeEnum;
import entity.pojo.info.Banner;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Banner响应VO
 * 用于返回给前端的Banner信息
 */
@Data
public class BannerVO {

    /**
     * Banner ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 链接URL
     */
    private String linkUrl;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 状态（0：禁用，1：启用）
     */
    private Integer status;

    /**
     * 交互行为类型
     */
    private BannerActionTypeEnum actionType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 从Banner实体转换为BannerVO
     * @param banner Banner实体
     * @return BannerVO
     */
    public static BannerVO fromBanner(Banner banner) {
        BannerVO bannerVO = new BannerVO();
        bannerVO.setId(banner.getId());
        bannerVO.setTitle(banner.getTitle());
        bannerVO.setImageUrl(banner.getImageUrl());
        bannerVO.setLinkUrl(banner.getLinkUrl());
        bannerVO.setSortOrder(banner.getSortOrder());
        bannerVO.setStatus(banner.getStatus());
        bannerVO.setActionType(BannerActionTypeEnum.getByValue(banner.getActionType()));
        bannerVO.setTargetId(banner.getTargetId());
        bannerVO.setCreateTime(banner.getCreateTime());
        bannerVO.setUpdateTime(banner.getUpdateTime());
        return bannerVO;
    }
}
