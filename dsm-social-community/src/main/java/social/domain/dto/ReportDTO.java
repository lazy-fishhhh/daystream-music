package social.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportDTO {
    /**
     * 目标Id
     */
    @NotBlank(message = "目标Id不能为空")
    private String targetId;
    /**
     * 举报目标类型 1-音乐 2-评论 3-用户
     */
    @NotNull(message = "举报目标类型不能为空")
    private Integer targetType;
    /**
     * 举报原因 1-色情低俗 2-违法信息 3-人身攻击 4-广告 5-侵权
     */
    @NotNull(message = "举报原因不能为空")
    private Integer reason;
    /**
     * 详情
     */
    @NotBlank(message = "详情不能为空")
    private String reportContent;

}
