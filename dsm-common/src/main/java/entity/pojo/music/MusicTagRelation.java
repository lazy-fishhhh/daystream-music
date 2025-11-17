package entity.pojo.music;

import lombok.Data;

@Data
public class MusicTagRelation {
    private Long id;
    private Long musicId;
    private Long tagId;
}
