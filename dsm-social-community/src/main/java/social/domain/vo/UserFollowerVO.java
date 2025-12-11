package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserFollowerVO {
    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户姓名
     */
    private String username;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 用户简介
     */
    private String introduction;
    /**
     * 用户状态 0 封禁 1-正常
     */
    private Integer userStatus;
    /**
     * 关注时间
     */
    private LocalDateTime followTime;
    /**
     * 动态数量
     */
    private Long dynamicCount;
    /**
     * 歌单数量
     */
    private Long playListCount;
    /**
     * 粉丝数量
     */
    private Long fansCount;
}
