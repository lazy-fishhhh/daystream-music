package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatGroupVO {
    
    private Long id;
    
    private String name;
    
    private String avatar;
    
    private Long ownerId;
    
    private String notice;
    
    private Integer memberCount;
    
    private LocalDateTime createdAt;
}
