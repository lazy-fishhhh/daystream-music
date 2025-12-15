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
    private String musicName;          // 歌曲名称（模糊查询）
    private String musicianName;       // 音乐人名称（模糊查询）
    private Integer status;            // 状态（0-待审核，1-上架，2-下架）
    private Long albumId;              // 所属专辑ID
    private Long musicianId;           // 音乐人ID
}
