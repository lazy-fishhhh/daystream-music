package social.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentReplyDTO {
    /**
     * 回复评论的id
     */
    @NotBlank(message = "回复的评论id不能为空")
    private String parentId;
    /**
     * 评论内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;
}
