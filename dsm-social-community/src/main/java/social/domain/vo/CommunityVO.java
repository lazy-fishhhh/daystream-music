package social.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    @JsonIgnore
    private LocalDateTime vipExpireTime;
    /**
     * 动态id
     */
    private String postId;
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
    /**
     * 是否关注
     */
    private Boolean isFollow;
    /**
     * 是否是vip
     */
    private Boolean isVip;
    /**
     * 是否是我本人的动态
     */
    private Boolean isMine;
    /**
     * 动态图片
     */
    private List<String> photos;
}
