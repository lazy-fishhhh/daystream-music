package admin.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 推荐位管理DTO
 * 用于运营管理的5歌单30歌曲推荐位添加、编辑操作
 */
@Data
@ApiModel(description = "推荐位管理DTO")
public class RecommendPositionDTO {

    /**
     * 推荐位ID（编辑时必填）
     */
    @ApiModelProperty(value = "推荐位ID（编辑时必填）")
    private Long id;

    /**
     * 推荐位类型（1：歌单推荐，2：歌曲推荐）
     */
    @ApiModelProperty(value = "推荐位类型（1：歌单推荐，2：歌曲推荐）", required = true)
    private Integer type;

    /**
     * 关联的歌单/歌曲ID
     */
    @ApiModelProperty(value = "关联的歌单/歌曲ID", required = true)
    private Long itemId;

    /**
     * 排序位置（1-30）
     */
    @ApiModelProperty(value = "排序位置（1-30）", required = true)
    private Integer position;

    /**
     * 是否启用（0：禁用，1：启用）
     */
    @ApiModelProperty(value = "是否启用（0：禁用，1：启用）", required = true)
    private Integer status;
}
