package admin.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AdminCommentDTO {
    /**
     * 评论类型
     */
    private Integer type;
    /**
     * 搜索关键字
     */
    private String keyword;
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum=1;
    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize=10;
}
