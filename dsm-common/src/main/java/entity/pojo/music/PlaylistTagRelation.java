package entity.pojo.music;

import lombok.Data;

@Data
public class PlaylistTagRelation {
    private Long id;
    private Long playlistId;
    private Long tagId;
}
