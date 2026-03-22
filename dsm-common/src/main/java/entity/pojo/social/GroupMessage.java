package entity.pojo.social;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_message")
public class GroupMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long groupId;
    
    private Long fromUser;
    
    private String content;
    
    private String type;
    
    private String mentionedUserIds;
    
    private LocalDateTime sendTime;
    
    private Integer isRecalled;
    
    private LocalDateTime recallTime;
}
