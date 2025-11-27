package entity.pojo.music;

import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicTag {
    private Long id;
    private String tagName;
    private Integer tagType;
}
