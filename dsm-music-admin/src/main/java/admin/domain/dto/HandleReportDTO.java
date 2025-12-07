package admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandleReportDTO {
    /**
     * 举报id
     */
    @NotBlank(message = "举报id不能为空")
    private String reportId;
    /**
     * 举报目标id
     */
    @NotBlank(message = "举报目标id不能为空")
    private String targetId;
    /**
     * 举报目标类型 1-音乐 2-评论 3-用户 4-音乐人
     */
    @NotNull(message = "举报目标类型不能为空")
    private Integer targetType;
    /**
     * 处理结果 0-待处理 1-已通过 2-已驳回
     */
    @NotNull(message = "处理结果不能为空")
    private Integer handleResult;
}
