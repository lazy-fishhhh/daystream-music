package social.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏专辑表
 */
@Data
public class CollectioAlbumVO {
    /**
     * 专辑id
     */
    private Long Id;
    /**
     * 专辑名
     */
    private String albumName;
    /**
     * 作者id
     */
    private Long musicianId;
    /**
     * 作者名
     */
    private String userName;
    /**
     * 封面url
     */
    private String coverUrl;
    /**
     * 发行时间
     */
    private LocalDateTime releaseDate;
    /**
     * 状态 0-下架 1-上架
     */
    private Integer status;

}
