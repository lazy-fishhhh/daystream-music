package entity.pojo.social;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_group")
public class ChatGroup {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String avatar;
    
    private Long ownerId;
    
    private String notice;
    
    private Integer memberCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
