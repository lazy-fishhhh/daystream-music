package social.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知VO
 * 用于返回通知信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {
    
    /**
     * 通知ID
     */
    private Long id;
    
    /**
     * 发送用户ID
     */
    private Long fromUserId;
    
    /**
     * 发送用户昵称
     */
    private String fromUserName;
    
    /**
     * 发送用户头像
     */
    private String fromUserAvatar;
    
    /**
     * 通知类型：1-点赞，2-评论，3-粉丝
     */
    private Integer type;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 相关对象ID
     */
    private Long targetId;
    
    /**
     * 相关对象类型
     */
    private Integer targetType;
    
    /**
     * 目标对象名称
     */
    private String targetName;
    
    /**
     * 目标对象内容
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
