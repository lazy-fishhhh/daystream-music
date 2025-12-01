package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityVO {
    /**
     * 作者id
     */
    private Long userId;
    /**
     * 作者姓名
     */
    private String userName;
    /*
    *作者头像
     */
    private String avatar;
    /**
     * 作者简介
     */
    private String introduction;
    /**
     * vip过期时间(null为非vip)
     */
    private LocalDateTime vipExpireTime;
    /**
     * 动态id
     */
    private Long postId;
    /**
     * 动态内容
     */
    private String content;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 动态标题
     */
    private String title;
    /**
     * 点赞数
     */
    private Long likeCount;
    /**
     * 评论数
     */
    private Long commentCount;
    /**
     * 是否点赞
     */
    private Boolean isLike;
}
