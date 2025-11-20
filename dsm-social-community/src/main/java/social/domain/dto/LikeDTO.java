package social.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 点赞
 */
@Data
public class LikeDTO {
    /**
     * 要点赞的Id
     */
    @NotNull(message = "点赞目标id不能为空")
    private String targetId;
    /**
     * 点赞类型的id 1-音乐 2-评论
     */
    @NotNull(message = "点赞类型不为空")
    private Integer targetType;
}
