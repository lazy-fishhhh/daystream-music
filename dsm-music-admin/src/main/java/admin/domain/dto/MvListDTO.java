package admin.domain.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * MV列表查询条件
 */
@Data
public class MvListDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;       // 页码，默认1
    private Integer pageSize = 10;     // 每页数量，默认10
    private String mvName;             // MV名称（模糊查询）
    private String musicName;          // 音乐名称（模糊查询）
    private String musicianName;       // 音乐人名称（模糊查询）
    @Min(value = 0, message = "状态值只能为0-待审核，1-上架，2-下架")
    @Min(value = 2, message = "状态值只能为0-待审核，1-上架，2-下架")
    private Integer status;            // 状态（0-待审核，1-上架，2-下架）
    private Long musicId;              // 所属音乐ID
    private Long musicianId;           // 音乐人ID
}