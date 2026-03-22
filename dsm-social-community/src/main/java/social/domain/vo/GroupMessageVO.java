package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMessageVO {
    
    private Long id;
    
    private Long groupId;
    
    private Long fromUser;
    
    private String content;
    
    private String type;
    
    private String mentionedUserIds;
    
    private LocalDateTime sendTime;
    
    private Integer isRecalled;
}
