package social.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupMessageDTO {
    
    @NotNull(message = "群组ID不能为空")
    private Long groupId;
    
    @NotBlank(message = "消息内容不能为空")
    private String content;
    
    private String type = "text";
    
    private String mentionedUserIds;
}
