package entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicRecommendVO {
    private Long id;                    // 主键ID
    private String musicName;           // 歌曲名
    private String coverUrl;            // 封面URL
    private Long playCount;
    private Long downloadCount;
    private Long likeCount;             // 点赞量
    private Long commentCount;          // 评论量
    private LocalDateTime createTime;

    private String musicianName;        // 作者名字（音乐人名称）
}
