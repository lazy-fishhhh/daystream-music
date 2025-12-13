package admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMusicianDTO {
    /**
     * 音乐人id
     */
    @NotBlank(message = "音乐人id不能为空")
    private String id;
    /**
     * 艺名
     */
    private String stageName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 简介
     */
    private String introduction;
    /**
     * 音浪数
     */
    private Integer walletBalance;
    /**
     * 会员到期时间
     */
    private String vipExpireTime;
}
