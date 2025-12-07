package admin.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ReportDTO {
    /**
     * 搜索关键字
     */
    private String key;
    /**
     * 状态查找 0-待处理 1-审核通过 2-审核驳回
     */
    private Integer status;
    /**
     * 举报类型 1-音乐 2-评论 3-用户
     */
    private Integer type;
    /**
     * 举报原因 1-色情低俗 2-违法信息 3-人身攻击 4-广告 5-侵权
     */
    private Integer reason;
    /**
     * 页码
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum=1;
    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize=10;

}
