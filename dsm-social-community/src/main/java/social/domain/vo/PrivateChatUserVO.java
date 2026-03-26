package social.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私聊对象VO
 * 用于返回用户的私聊对象信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateChatUserVO {
    
    /**
     * 对方用户ID
     */
    private Long userId;
    
    /**
     * 对方用户昵称
     */
    private String nickname;
    
    /**
     * 对方用户头像
     */
    private String avatar;
    
    /**
     * 最后一条消息内容
     */
    private String lastMessage;
    
    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 未读消息数量
     */
    private Integer unreadCount;
    
    /**
     * 会话ID
     */
    private Long sessionId;
}
