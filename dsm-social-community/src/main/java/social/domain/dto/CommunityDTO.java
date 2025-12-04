package social.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CommunityDTO {
    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;
    /**
     * 照片url
     **/
    private List<String> images;
}
