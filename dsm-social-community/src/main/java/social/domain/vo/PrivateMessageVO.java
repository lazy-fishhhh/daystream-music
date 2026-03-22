package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateMessageVO {
    
    private Long id;
    
    private Long fromUser;
    
    private Long toUser;
    
    private String content;
    
    private String type;
    
    private LocalDateTime sendTime;
    
    private Integer isRecalled;
}
