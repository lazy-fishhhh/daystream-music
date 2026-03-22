package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateSessionVO {
    
    private Long id;
    
    private Long targetUserId;
    
    private String targetUserNickname;
    
    private String targetUserAvatar;
    
    private String lastMessage;
    
    private LocalDateTime lastMsgTime;
    
    private Integer unreadCount;
}
