package social.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MyPostDTO {
    /**
     * 动态状态 0-未删除 1-已删除
     */
    private Integer status;
    /**
     * 搜索关键字(模糊匹配标题，内容）
     */
    private String keyword;
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
