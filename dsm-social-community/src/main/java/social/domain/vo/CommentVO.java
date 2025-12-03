package social.domain.vo;

import lombok.Data;
import social.domain.dto.CommentListDTO;

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
    /**
     * 评论详情
     */
    private CommentListVO detailComment;
}
