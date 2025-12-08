package admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.Music;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMusicMapper extends BaseMapper<Music> {
}
