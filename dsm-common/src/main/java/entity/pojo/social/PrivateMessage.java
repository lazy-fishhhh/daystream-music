package entity.pojo.social;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message")
public class PrivateMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sessionId;
    
    private Long fromUser;
    
    private Long toUser;
    
    private String content;
    
    private String type;
    
    private LocalDateTime sendTime;
    
    private Integer delivered;
    
    private LocalDateTime readTime;
    
    private Integer isRecalled;
    
    private LocalDateTime recallTime;
}
