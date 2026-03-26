package social.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息计数VO
 * 用于实时推送给前端的未读消息数量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {
    
    /**
     * 会话类型：private-私聊，group-群聊
     */
    private String type;
    
    /**
     * 会话ID：私聊为对方用户ID，群聊为群ID
     */
    private Long sessionId;
    
    /**
     * 未读消息数量
     */
    private Integer unreadCount;
    
    /**
     * 最后一条消息内容
     */
    private String lastMessage;
    
    /**
     * 最后消息时间
     */
    private Long timestamp;
}
