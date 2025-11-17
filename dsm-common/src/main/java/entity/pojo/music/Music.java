package entity.pojo.music;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.Transient;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Music {
    private Long id;                 // 主键ID
    private String musicName;        // 歌曲名
    private Long albumId;            // 所属专辑ID
    private Long musicianId;         // 音乐人ID
    private String duration;        // 时长（秒）
    private String lyricUrl;         // 歌词URL
    private String coverUrl;         // 封面URL
    private Integer bpm;             // 节奏（BPM）
    private String licenseType;      // 版权类型
    private Integer isVip;           // 是否VIP专属（0-否，1-是）
    private BigDecimal price;        // 单价（音浪）
    private Long playCount;          // 播放量
    private Long downloadCount;      // 下载量
    private Integer status;          // 状态（0-待审核，1-上架，2-下架）
    private Long likeCount;          //点赞量
    private Long collectCount;        //收藏量
    private Long commentCount;        //评论量
    @TableField(exist = false)
    private Integer isLike;          //是否点赞（0-未点赞，1-已点赞）
    @TableField(exist = false)
    private Integer isCollection;    //是否收藏（0-未收藏，1-已收藏）
    @TableField(exist = false)
    private List<MusicAudio> audioList; // 音频列表
}