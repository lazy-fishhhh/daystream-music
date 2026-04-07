package social.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollectionDTO {
    /**
     * 收藏目标id
     */
    @NotBlank(message = "收藏目标id不为空")
    private String targetId;
    /**
     * 收藏类型 1-歌单 2-专辑
     */
    @NotNull(message = "收藏类型不为空")
    private Integer targetType;
}
