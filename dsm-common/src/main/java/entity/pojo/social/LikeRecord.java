package entity.pojo.social;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeRecord {
    @TableId(type= IdType.AUTO)
    private Long id;                 // 主键ID
    private Long userId;             // 点赞用户ID
    private Integer targetType;      // 点赞目标类型（1-音乐，2-评论，3-歌单）
    private Long targetId;           // 目标ID
    private Integer isCanceled;      // 是否取消点赞（0-有效，1-已取消）
    private LocalDateTime createdTime; // 点赞时间
    private LocalDateTime updatedTime; // 更新时间
}