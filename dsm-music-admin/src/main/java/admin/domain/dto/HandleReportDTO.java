package admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandleReportDTO {
    /**
     * id
     */
    @NotBlank(message = "id不能为空")
    private String reportId;
    /**
     * 处理结果 1-已通过 2-已驳回
     */
    @NotNull(message = "处理结果不能为空")
    private Integer handleResult;
}
