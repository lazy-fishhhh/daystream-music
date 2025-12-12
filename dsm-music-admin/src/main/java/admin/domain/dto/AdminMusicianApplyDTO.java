package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
@Data
public class AdminMusicianApplyDTO {
    /**
     * 批量操作的审批id
     */
    @NotNull(message = "批量操作的审批id不能为空")
    private List<Long> applyId;
}
