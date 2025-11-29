package social.domain.vo;

import lombok.Data;

@Data
public class DeleteCommentVO {
    /**
     * 父评论的回复数
     */
    private Long replyCount;
    /**
     * 目标的评论数
     */
    private Long targetCount;
}
