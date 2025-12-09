package social.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.Playlist;
import entity.pojo.music.PlaylistMusic;
import org.mapstruct.Mapper;

@Mapper
public interface CommunityPlaylistMapper extends BaseMapper<PlaylistMusic> {
}
