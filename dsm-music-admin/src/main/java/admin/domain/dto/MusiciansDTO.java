package admin.domain.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MusiciansDTO {
    /**
     * 搜索关键字
     */
    private String key;
    /**
     * 状态查找 0-待审核 1-审核通过 2-审核驳回
     */
    @NotNull(message = "请选择状态")
    private Integer status;
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
