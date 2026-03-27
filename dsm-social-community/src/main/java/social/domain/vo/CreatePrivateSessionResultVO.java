package social.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建私聊会话结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrivateSessionResultVO {
    /**
     * 创建是否成功
     */
    private boolean success;
    
    /**
     * 会话ID
     */
    private Long sessionId;
    
    /**
     * 对方用户信息
     */
    private OtherUserVO otherUser;
}