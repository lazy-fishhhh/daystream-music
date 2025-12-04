package social.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePostDTO {
    /**
     * 要修改的动态id
     */
    @NotBlank(message = "要修改的动态id不能为空")
    private String postId;
    /**
     * 修改后的内容
     */
    private String content;
    /**
     * 修改后的照片
     */
    @NotNull(message = "修改后的照片不能为空")
    private List<String> images;
    /**
     * 修改后的标题
     */
    private String title;
}
