package social.domain.vo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞歌曲表
 */
@Data
public class LikeMusicVO {
    /**
     *歌曲id
     */
    private Long id;
    /**
     * 歌曲名
     */
    private String musicName;
    /**
     * 所属专辑id
     */
    private Long albumId;
    /**
     * 专辑名
     */
    private String albumName;
    /**
     * 创作者Id
     */
    private Long musicianId;
    /**
     * 创作者姓名
     */
    private String musicianName;
    /**
     * 封面url
     */
    private String coverUrl;
    /**
     * 是否vip专属 0-否 1-是
     */
    private Integer isVip;
    /**
     * 歌曲时长
     */
    private String duration;
    /**
     * 是否下架 1-上架 2-下架
     */
    private Integer status;
    @JsonIgnore
    private LocalDateTime updatedTime;
}
