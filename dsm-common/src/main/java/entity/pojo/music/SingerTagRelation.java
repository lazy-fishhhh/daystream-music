package entity.pojo.music;

import lombok.Data;

import java.time.LocalDateTime;
@Data
/**
 * 歌手标签表关联表
 */
public class SingerTagRelation {
    private Long id;//主键id
    private Long musicianId;//歌手id
    private Integer tagId;//标签id
    private LocalDateTime createdTime;//创建时间
}
