package social.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 离线消息摘要VO
 * 用于返回用户上线时的离线消息摘要信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfflineMessageSummaryVO {
    
    /**
     * 会话类型：private-私聊，group-群聊
     */
    private String type;
    
    /**
     * 会话ID：私聊为对方用户ID，群聊为群ID
     */
    private Long sessionId;
    
    /**
     * 离线消息数量
     */
    private Integer offlineCount;
    
    /**
     * 最新一条消息内容
     */
    private String lastMessage;
    
    /**
     * 最新消息时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 对方用户ID（私聊时）
     */
    private Long otherUserId;
    
    /**
     * 对方用户昵称（私聊时）
     */
    private String otherUserName;
    
    /**
     * 对方用户头像（私聊时）
     */
    private String otherUserAvatar;
    
    /**
     * 群聊名称（群聊时）
     */
    private String groupName;
    
    /**
     * 群聊头像（群聊时）
     */
    private String groupAvatar;
}
