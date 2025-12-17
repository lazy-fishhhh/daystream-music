package admin.domain.dto;

import admin.domain.enums.BannerActionTypeEnum;
import annotation.IsZeroOrOne;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Banner管理DTO
 * 用于运营管理的轮播图添加、编辑操作
 */
@Data
@ApiModel(description = "Banner管理DTO")
public class BannerDTO {

    /**
     * Banner ID（编辑时必填）
     */
    @ApiModelProperty(value = "Banner ID（编辑时必填）")
    private Long id;

    /**
     * Banner标题
     */
    @ApiModelProperty(value = "Banner标题", required = true)
    private String title;

    /**
     * Banner图片URL
     */
    @ApiModelProperty(value = "Banner图片URL", required = true)
    private String imageUrl;

    /**
     * 跳转链接
     */
    @ApiModelProperty(value = "跳转链接", required = true)
    private String linkUrl;

    /**
     * 排序权重（数值越大越靠前）
     */
    @ApiModelProperty(value = "排序权重（数值越大越靠前）", required = true)
    @Min(value = 0, message = "排序权重不能小于0")
    private Integer sortOrder;

    /**
     * 是否启用（0：禁用，1：启用）
     */
    @ApiModelProperty(value = "是否启用（0：禁用，1：启用）", required = true)
    @IsZeroOrOne
    private Integer status;

    /**
     * 交互行为类型
     */
    @ApiModelProperty(value = "交互行为类型", required = true)
    private BannerActionTypeEnum actionType;

    /**
     * 目标ID（根据actionType关联：1=歌单ID，2=歌曲ID，3=活动ID，0=无）
     */
    @ApiModelProperty(value = "目标ID（根据actionType关联：1=歌单ID，2=歌曲ID，3=活动ID，0=无）", required = true)
    private Long targetId;
}
