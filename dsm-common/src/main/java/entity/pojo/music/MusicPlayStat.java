package entity.pojo.music;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("music_play_stat")
public class MusicPlayStat {
    private Long id;
    private Long userId;        // 用户ID（未登录为null）
    private Long musicId;       // 音乐ID
    private LocalDateTime createTime; // 统计时间
}