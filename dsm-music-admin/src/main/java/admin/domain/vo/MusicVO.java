package admin.domain.vo;

import entity.pojo.music.MusicAudio;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 音乐作品视图对象
 */
@Data
public class MusicVO {
    private Long id;                 // 主键ID
    private String musicName;        // 歌曲名
    private Long albumId;            // 所属专辑ID
    private String albumName;        // 专辑名称
    private Long musicianId;         // 音乐人ID
    private String musicianName;     // 音乐人名称
    private Integer duration;        // 时长
    private String coverUrl;         // 封面URL
    private Integer bpm;             // 节奏（BPM）
    private String licenseType;      // 版权类型
    private Integer isVip;           // 是否VIP专属（0-否，1-是）
    private BigDecimal price;        // 单价（音浪）
    private Long playCount;          // 播放量
    private Long downloadCount;      // 下载量
    private Integer status;          // 状态（0-待审核，1-上架，2-下架）
    private Long likeCount;          //点赞量
    private Long commentCount;       //评论量
    private LocalDateTime createTime; // 创建时间
    private List<MusicAudio> audioList; // 音频类型列表
    private Integer isLiked;         // 是否被当前用户点赞过（0-否，1-是）
}
