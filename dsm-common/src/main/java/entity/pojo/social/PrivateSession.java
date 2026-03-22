package entity.pojo.social;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_session")
public class PrivateSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long user1Id;
    
    private Long user2Id;
    
    private String lastMessage;
    
    private LocalDateTime lastMsgTime;
    
    private Long lastMsgId;
    
    private Integer unreadCount1;
    
    private Integer unreadCount2;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
