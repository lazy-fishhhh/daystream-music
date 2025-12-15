package admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.Album;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminAlbumMapper extends BaseMapper<Album> {
}