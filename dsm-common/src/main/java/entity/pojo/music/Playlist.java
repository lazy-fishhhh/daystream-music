package entity.pojo.music;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

/**
 * 播放列表实体类
 * 对应数据库中的播放列表表，存储用户创建的音乐播放列表信息
 */
public class Playlist {
    /** 主键ID */
    private Long id;

    /** 创建者用户ID */
    private Long userId;

    /** 列表名称 */
    @TableField("playlist_name")
    private String name;

    /** 封面图片URL */
    @TableField("cover_url")
    private String cover;

    /** 描述信息 */
    private String description;

    /** 是否公开（0-私有，1-公开） */
    private int isPublic;

    /** 包含音乐数量 */
    private Long musicCount;

    /** 播放总次数 */
    private Long playCount;

    /** 创建时间 */
    @TableField("created_time")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField("updated_time")
    private LocalDateTime updatedTime;

    /** 点赞数 */
    private Long likeCount;

    /** 收藏数 */
    private Long collectCount;

    /** 评论数 */
    private Long commentCount;
}
