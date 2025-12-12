package admin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DetailUserVO {
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
     * 用户角色
     */
    private String role;
    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 简介
     */
    private String introduction;
    /**
     * 性别 0-未知 1-男 2-女
     */
    private Integer gender;
    /**
     * 会员到期时间
     */
    private LocalDateTime vipExpireTime;
    /**
     * 音浪数
     */
    private Integer walletBalance;
}
