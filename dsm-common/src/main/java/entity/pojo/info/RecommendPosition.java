package entity.pojo.info;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐位实体类
 * 用于运营管理的5歌单30歌曲推荐位管理
 */
@Data
@TableName("recommend_position")
public class RecommendPosition {

    /**
     * 推荐位ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 推荐位类型（1：歌单推荐，2：歌曲推荐）
     */
    private Integer type;

    /**
     * 关联的歌单/歌曲ID
     */
    private Long itemId;

    /**
     * 排序位置（1-30）
     */
    private Integer position;

    /**
     * 是否启用（0：禁用，1：启用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
