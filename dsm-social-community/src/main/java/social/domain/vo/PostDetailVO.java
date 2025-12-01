package social.domain.vo;

import lombok.Data;

@Data
public class PostDetailVO {
    /**
     * 动态id
     */
    private String postId;
    /**
     * 动态标题
     */
    private String title;
    /**
     * 动态内容
     */
    private String content;
    /**
     * 点赞数
     */
    private Long likeCount;
    /**
     * 评论数
     */
    private Long commentCount;
    /**
     * 动态作者id
     */
    private Long authorId;
    /**
     * 动态作者昵称
     */
    private String authorName;
    /**
     * 动态作者头像
     */
    private String avatar;
     /**
     * 动态发布时间
     */
    private String createTime;
    /**
     * 是否关注作者
     */
    private Boolean isFollow;
    /**
     * 是否点赞
     */
    private Boolean isLike;
    /**
     * 关注数
     */
    private Long followCount;
    /**
     * 粉丝数
     */
    private Long followerCount;
}
