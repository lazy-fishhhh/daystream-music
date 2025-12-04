package entity.pojo.social;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostPhotos {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 动态id
     */
    private Long postId;
    /**
     * 照片地址
     */
    private String photoUrl;
    /**
     * 排序
     */
    private Integer sortOrder;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
