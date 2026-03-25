package social.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读通知数量VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadNotificationCountVO {
    private Integer likeCount;
    private Integer commentCount;
    private Integer followCount;
    private Integer privateMessageCount;
}
