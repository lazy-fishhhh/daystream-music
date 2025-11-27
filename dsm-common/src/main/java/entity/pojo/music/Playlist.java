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
    private Long id;
    private Long userId;
    @TableField("playlist_name")
    private String name;
    @TableField("cover_url")
    private String cover;
    /** 描述信息 */
    private String description;
    /** 是否公开（0-私有，1-公开） */
    private int isPublic;
    private Long musicCount;
    private Long playCount;
    @TableField("created_time")
    private LocalDateTime createdTime;
    @TableField("updated_time")
    private LocalDateTime updatedTime;
    private Long likeCount;
    private Long collectCount;
    private Long commentCount;
}
