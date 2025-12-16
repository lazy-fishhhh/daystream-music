package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CommentBatchDTO {
    /**
     * 评论id
     */
    @NotNull(message = "id不能为空")
    private List<Long> commentId;
}
