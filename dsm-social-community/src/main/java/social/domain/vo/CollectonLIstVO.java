package social.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏歌单表
 */
@Data
public class CollectonLIstVO {
    /**
     * 歌单id
     */
    private Long Id;
    /**
     * 歌单名
     */
    private String playlistName;
    /**
     * 封面url
     */
    private String coverUrl;
    /**
     * 是否公开 0-私有 1-公开
     */
    private Integer isPublic;
    /**
     * 歌曲数量
     */
    private Long musicCount;
    /**
     * 播放量
     */
    private Long playCount;
    /**
     * 作者id
     */
    private Long userId;
    /**
     * 作者姓名
     */
    private String userName;
    /**
     * 歌单描述
     */
    private String description;
    @JsonIgnore
    private LocalDateTime updatedTime;
}
