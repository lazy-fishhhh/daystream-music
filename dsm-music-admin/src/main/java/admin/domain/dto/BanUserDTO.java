package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BanUserDTO {
    /**
     * 用户Id
     */
    @NotNull(message = "用户Id不能为空")
    private Long userId;
    /**
     * 用户状态 0-封禁 1-正常
     */
    @NotNull(message = "用户状态不能为空")
    private Integer status;
}
