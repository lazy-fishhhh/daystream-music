package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCommentDTO {
    /**
     * 评论id
     */
    @NotNull(message = "id不能为空")
    private Long id;
    /**
     * 评论内容
     */
    private String content;
}
