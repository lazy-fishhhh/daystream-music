package admin.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MusicianListDTO {
    /**
     * 音乐人名字key
     */
    private String nameKey;
    /**
     *  状态 0-正常 2-封禁
     */
    private Integer status;
    /**
     * 页码
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum=1;
    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize=10;
}
