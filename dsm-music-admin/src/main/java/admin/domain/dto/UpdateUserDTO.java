package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserDTO {
    /**
     * 用户id
     */
    @NotNull(message = "用户id不能为空")
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
     * 用户简介
     */
    private String introduction;
    /**
     * 性别 0-未知 1-男 2-女
     */
    private Integer gender;
    /**
     * 音浪数
     */
    private Integer walletBalance;
    /**
     * 会员到期时间
     */
    private String vipExpireTime;
}
