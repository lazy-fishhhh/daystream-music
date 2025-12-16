package admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePostDTO {
    /**
     * 动态Id
     */
    @NotBlank(message = "动态Id不能为空")
    private String id;
    /**
     * 动态标题
     */
    private String title;
    /**
     * 动态内容
     */
    private String content;
    /**
     * 动态状态 0-正常 2-封禁下架
     */
    private Integer status;
}
