package social.domain.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UserFollow {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 关注者Id
     */
    private Long userId;
    /**
     * 被关注者Id
     */
    private Long followedUserId;
    /**
     * 关注时间
     */
    private LocalDateTime followTime;
}
