package social.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在线状态变更VO
 * 用于增量推送用户上下线状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineStatusVO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 状态：0-离线，1-在线
     */
    private Integer status;
    
    /**
     * 变更时间
     */
    private Long timestamp;
}
