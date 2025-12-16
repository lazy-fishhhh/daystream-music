package admin.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
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
     * 用户状态
     */
    private String status;
    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 是否是vip
     */
    private Boolean isVip;
    @JsonIgnore
    /**
     * vip到期时间
     */
    private LocalDateTime vipExpireTime;

}
