package admin.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 音乐作品列表查询条件
 */
@Data
public class MusicListDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;       // 页码，默认1
    private Integer pageSize = 10;     // 每页数量，默认10
    private String keyword;            // 关键字（模糊查询，匹配歌曲名或音乐人名）
    private Integer status;            // 状态（0-待审核，1-上架，2-下架）
    private Long albumId;              // 所属专辑ID
    private Long musicianId;           // 音乐人ID
}
