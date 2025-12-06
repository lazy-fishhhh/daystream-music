package social.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReplyDTO {
    /**
     * 父评论id
     */
    @NotBlank(message = "父评论不能为空")
    private String parentId;
    /**
     * 上一页最后一条的id,第一次获取为null
     */
    private String lastId;
    /**
     * 获取条数
     */
    @Min(value = 1, message = "获取条数不能小于1")
    @Max(value = 100,message = "获取条数不能大于100")
    private Integer size=10;

}
