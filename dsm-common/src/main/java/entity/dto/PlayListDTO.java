package entity.dto;


import annotation.IsZeroOrOne;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayListDTO {
    @ApiModelProperty(value = "歌单ID（创建歌单不传，更新要传）")
    private Long id;
    @TableField("playlist_name")
    @NotNull(message = "歌单名称不能为空")
    private String name;
    @TableField("cover_url")
    @NotNull(message = "歌单封面不能为空")
    private String cover;
    @NotNull(message = "歌单描述不能为空")
    private String description;
    @ApiModelProperty(value = "是否公开（1-公开，0-私有，默认1）")
    @IsZeroOrOne //自定义注解
    private int isPublic;
    @NotNull(message = "歌单标签不能为空")
    private int tagId;
}
