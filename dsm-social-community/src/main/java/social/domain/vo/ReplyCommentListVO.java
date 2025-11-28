package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReplyCommentListVO {
    /**
     * 评论id
     */
    private Long id;
    /**
     * 评论内容
     */
    private String content;
    /**
     * 评论用户id
     */
    private Long userId;
    /**
     * 评论用户昵称
     */
    private String username;
    /**
     * 评论用户头像
     */
    private String avatar;
    /**
     * 评论时间
     */
    private LocalDateTime createdTime;
    /**
     * 评论点赞数
     */
    private Long likeCount;
    /**
     * 评论是否点赞
     */
    private Boolean isLike;
    /**
     * 是否是本人发的评论
     */
    private Boolean isSelf;
    /**
     * 被回复者的id
     */
    private Long replyUserId;
    /**
     * 被回复者的昵称
     */
    private String replyUsername;
}
