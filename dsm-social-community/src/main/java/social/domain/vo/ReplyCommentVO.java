package social.domain.vo;

import lombok.Data;

@Data
public class ReplyCommentVO {
    /**
     * 状态描述
     */
    private String msg;
    /**
     * 目标评论数
     */
    private Long targetCommentCount;
    /**
     * 父评论回复数
     */
    private Long parentCommentReplyCount;
    /**
     * 评论的详细信息
     */
    private ReplyCommentListVO detailReply;
}
