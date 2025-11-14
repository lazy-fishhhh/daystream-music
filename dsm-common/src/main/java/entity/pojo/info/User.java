package entity.pojo.info;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @TableId(type=IdType.AUTO)
    private Long id;                 // 用户ID
    private String username;         // 用户名
    private String password;         // 密码（BCrypt加密）
    private String phone;            // 手机号
    private String email;            // 邮箱
    private String avatar;           // 头像URL
    private int gender;              // 性别（0-未知，1-男, 2-女）
    private String introduction;     // 个人简介
    private Integer userStatus;      // 状态（0-封禁，1-正常）
    private String lastLoginIp;      // 最后登录IP
    private Integer totalPoint;      // 累计积分
    private BigDecimal walletBalance;// 钱包余额（元）
    private LocalDateTime vipExpireTime; // VIP过期时间
    private LocalDateTime lastCheckinTime; // 最近签到日期
    private String referralCode;     // 邀请码
    private Integer freeVipUsed;     // 是否使用过免费会员（0-否，1-是）
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
    private LocalDateTime lastLoginTime; // 最后登录时间
}