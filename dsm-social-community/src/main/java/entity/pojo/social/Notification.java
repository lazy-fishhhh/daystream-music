package entity.pojo.social;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体
 * 用于存储用户的通知消息
 */
@Data
@TableName("notification")
public class Notification {
    
    /**
     * 通知ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 接收用户ID
     */
    private Long userId;
    
    /**
     * 发送用户ID
     */
    private Long fromUserId;
    
    /**
     * 通知类型：1-点赞，2-评论，3-粉丝
     */
    private Integer type;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 相关对象ID（如点赞的音乐ID、评论的评论ID等）
     */
    private Long targetId;
    
    /**
     * 相关对象类型（如1-音乐，2-评论，3-动态等）
     */
    private Integer targetType;
    
    /**
     * 目标对象名称（如音乐名称、歌单名称、专辑名称等）
     */
    private String targetName;
    
    /**
     * 目标对象内容（如评论内容、动态内容等）
     */
    private String targetContent;
    
    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 读取时间
     */
    private LocalDateTime readTime;
}
