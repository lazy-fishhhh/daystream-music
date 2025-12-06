package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserInfoVO {
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
     * vip过期时间
     */
    private LocalDateTime vipExpireTime;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 用户手机号
     */
    private String phone;
    /**
     * 积分
     */
    private Integer totalPoint;
    /**
     * 音浪
     */
    private Integer walletBalance;
    /**
     * 用户创建时间
     */
    private String createdTime;
    /**
     * 用户身份
     */
    private List<String> userRole;

    /**
     * 上次签到时间
     */
    private LocalDateTime lastCheckinTime;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    /**
     * 信息更新时间
     */
    private LocalDateTime updatedTime;


    /**
     * 背景图片
     */
    private String backgroundImage;

    /**
     * 关注数量
     */
    private Integer followCount;
    /**
     * 粉丝数量
     */
    private Integer fansCount;

    /**
     * 在线状态
     */
    private Integer onlineStatus;

    /*
     * 收获点赞数
     */
    private Integer likeCount;
}

