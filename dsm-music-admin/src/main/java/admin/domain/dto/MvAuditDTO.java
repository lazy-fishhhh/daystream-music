package admin.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * MV审核请求参数
 */
@Data
public class MvAuditDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;                   // MV ID
    private List<Long> ids;            // 批量审核的MV ID列表
    @Min(value = 1, message = "审核状态不能为空")
    @Max(value = 2, message = "审核状态不能为空")
    private Integer status;            // 审核状态（1-通过，2-驳回）
    private String reason;             // 审核驳回原因
}