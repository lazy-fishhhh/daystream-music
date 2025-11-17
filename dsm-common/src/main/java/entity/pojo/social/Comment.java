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
public class Comment {
    @TableId(type= IdType.AUTO)
    private Long id;                 // 评论ID
    private Integer targetType;      // 评论目标类型（1-音乐，2-歌单，3-专辑）
    private Long targetId;           // 目标ID
    private Long userId;             // 评论用户ID
    private String content;          // 评论内容
    private Long parentId;           // 父评论ID（0为一级评论）
    private Long replyToUserId;      // 回复目标用户ID
    private Long likeCount;       // 点赞数
    private Integer status;          // 状态（0-删除，1-正常）
    private LocalDateTime createdTime; // 评论时间
    private LocalDateTime updatedTime; // 更新时间
}