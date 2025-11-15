package entity.pojo.social;

import io.swagger.models.auth.In;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCollection {
    private Long id;
    private Long userId;
    private Integer targetType;
    private Long targetId;
    private Integer isCanceled; //是否取消收藏（0-正常收藏，1-已取消）
    private LocalDateTime collectedTime; // 点赞时间
    private LocalDateTime updatedTime; // 更新时间
}
