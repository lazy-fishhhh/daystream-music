package entity.pojo.music;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class MusicMv {
    private Long id;                 // MV主键ID
    private String mvName;           // MV名称
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long musicId;            // 关联音乐ID
    private Integer duration;        // MV时长（秒）
    private String coverUrl;         // MV封面URL（首帧截图）
    private LocalDateTime releaseTime; // MV发布时间
    private Long playCount;          // 播放量
    private Long likeCount;          // 点赞数（冗余）
    private Long commentCount;       // 评论数（冗余）
    private Long shareCount;         // 分享数
    private Integer isVip;           // 是否VIP专属（0-否，1-是）
    private Integer status;          // 状态（0-待审核，1-上架，2-下架）
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间


}