package social.domain.vo;

import lombok.Data;

@Data
public class CommentVO {
    /**
     * 状态描述
     */
    private String msg;
    /**
     * 目标评论数
     */
    private Long targetCommentCount;
}
