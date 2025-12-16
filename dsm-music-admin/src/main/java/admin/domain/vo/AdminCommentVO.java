package admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCommentVO {
    /**
     * 评论id
     */
    private Long id;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 用户昵称
     */
    private String username;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 评论内容
     */
    private String content;
    /**
     * 评论类型
     */
    private Integer type;
    /**
     * 评论对象id
     */
    private Long targetId;
    /**
     * 评论时间
     */
    private LocalDateTime createdTime;
}
