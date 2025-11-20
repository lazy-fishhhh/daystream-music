package entity.pojo.util;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageReq {
    /**
     * 当前页码
     */
    @NotNull // 对int无效
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;


    /**
     * 每页显示数量
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Integer pageSize = 10;
}