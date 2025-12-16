package admin.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostApplyDTO {
    /**
     * 搜索关键字
     */
    private String key;
    /**
     * 用户身份 1-普通用户 3-音乐人
     */

    private Integer role;
    /**
     * 审批状态 0-待处理 1-通过 2-驳回
     */
    @NotNull(message = "审理状态不能为空")
    private Integer status;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum=1;
    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize=10;
}
