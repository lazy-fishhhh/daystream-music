package social.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Data
public class OtherUserVO {
    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 用户性别
     */
    private String gender;
    /**
     * 用户简介
     */
    private String introduction;
    /**
     * 是否是vip
     */
    private Boolean isVip;
    /**
     * 关注数
     */
    private Long followCount;
    /**
     * 粉丝数
     */
    private Long followerCount;
    /**
     * 是否关注
     */
    private Boolean isFollow;
    @JsonIgnore
    private LocalDateTime vipExpireTime;
}
