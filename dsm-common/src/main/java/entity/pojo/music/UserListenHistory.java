package entity.pojo.music;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListenHistory {
    private Long id;                 // 主键ID
    private Long userId;             // 用户ID
    private Long musicId;            // 音乐ID
    private Integer totalListenSeconds;// 该歌曲当日累计有效时长（秒）
    private Integer listenCount;    // 该歌曲当日播放次数（达标一次算一次）
    private LocalDate listenTime;   // 听歌日期（按天分组）
}