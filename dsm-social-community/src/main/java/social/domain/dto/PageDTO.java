package social.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageDTO {
    /**
     * 上一页最后一条的id,第一次获取为null
     */
    private String lastId;
    /**
     * 每页的大小
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100,message = "获取条数不能大于100")
    private Integer size=10;
}
