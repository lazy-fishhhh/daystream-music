package social.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CommentListDTO {
    /**
     * 目标id
     */
    @NotBlank(message = "目标id不能为空")
    private String targetId;
    /**
     * 目标类型 1-音乐 2-歌单 3-专辑
     */
    @NotNull(message = "目标类型不能为空")
    private Integer targetType;
    /**
     * 上一页最后一条的id,第一次获取为null
     */
    private String lastId;
    /**
     * 获取条数
     */
    @Min(value = 1, message = "获取条数不能小于1")
    @Max(value = 100,message = "获取条数不能大于100")
    @NotNull(message = "每页大小不能为空")
    private Integer size;

}
