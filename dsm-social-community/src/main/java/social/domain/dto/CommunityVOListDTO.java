package social.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommunityVOListDTO {
    /**
     * 作者id
     */
    private Long userId;
    /**
     *关键字
     */
    private String keyword;
    /**
     * 上一页最后一个id 从头开始为null
     */
    private String lastId;
    /**
     * 每页条数
     */
    @Max(value = 100,message = "每页条数不能超过100")
    @Min(value = 1,message = "每页条数不能小于1")
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;
}
