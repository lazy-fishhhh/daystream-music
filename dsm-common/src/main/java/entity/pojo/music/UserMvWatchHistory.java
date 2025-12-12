package entity.pojo.music;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMvWatchHistory {
    private Long id;                 // 历史记录ID
    private Long userId;             // 用户ID
    private Long mvId;               // MV ID
    private Integer watchCount;      // 累计观看次数
    private LocalDateTime lastWatchTime; // 最后观看时间
}