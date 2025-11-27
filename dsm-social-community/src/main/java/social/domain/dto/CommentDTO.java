package social.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentDTO {
    /**
     * 要评论的音乐/专辑/歌单id/动态id
     */
    @NotBlank(message = "目标id不能为空")
    private String targetId;
    /**
     * 评论类型 1-音乐 2-歌单 3-专辑 4-社区动态
     */
    @NotNull(message = "评论类型不能为空")
    private Integer targetType;
    /**
     * 评论内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;
}
