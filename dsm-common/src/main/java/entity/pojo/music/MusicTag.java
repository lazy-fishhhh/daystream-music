package entity.pojo.music;

import io.swagger.models.auth.In;
import lombok.Data;

@Data
public class MusicTag {
    private Long id;
    private String tagName;
    private Integer tagType;
}
