package entity.pojo.info;

import annotation.IsZeroOrOne;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Banner实体类
 * 用于运营管理的轮播图管理
 */
@Data
@TableName("banner")
public class Banner {

    /**
     * Banner ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Banner标题
     */
    private String title;

    /**
     * Banner图片URL
     */
    private String imageUrl;

    /**
     * 跳转链接
     */
    private String linkUrl;

    /**
     * 排序权重（数值越大越靠前）
     */
    private Integer sortOrder;

    /**
     * 是否启用（0：禁用，1：启用）
     */
    private Integer status;

    /**
     * 交互行为类型（0-无操作，1-跳转到歌单，2-跳转到歌手，3-跳转到专辑，4-跳转到MV）
     */
    private Integer actionType;

    /**
     * 目标ID（根据actionType关联不同实体的ID）
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
}
