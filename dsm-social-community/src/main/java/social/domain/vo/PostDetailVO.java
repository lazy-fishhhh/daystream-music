package social.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private Long postlikeCount;
    /**
     * 作者的获赞数
     */
    private Long authorLikeCount;
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
     * 动态作者简介
     */
    private String introduction;
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
    /**
     * 是否是vip
     */
    private Boolean isVip;
    /*
    是否是我本人的动态
     */
    private Boolean isMine;
    /**
     * 动态照片
     */
    private List<String> photos;
    @JsonIgnore
    private LocalDateTime vipExpireTime;

}
