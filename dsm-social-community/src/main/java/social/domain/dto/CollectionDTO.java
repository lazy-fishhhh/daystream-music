package social.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollectionDTO {
    /**
     * 收藏目标id
     */
    @NotNull(message = "收藏目标id不为空")
    private String targetId;
    /**
     * 收藏类型 1-音乐 2 歌单
     */
    @NotNull(message = "收藏类型不为空")
    private Integer targetType;
}
