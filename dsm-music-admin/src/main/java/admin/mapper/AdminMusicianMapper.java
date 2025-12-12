package admin.mapper;

import admin.domain.vo.MusicianListVO;
import admin.domain.vo.MusicianVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.music.Musician;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMusicianMapper extends BaseMapper<Musician> {
    IPage<MusicianVO> getMusicianList(@Param("page") Page<MusicianVO> page,
                                      @Param("status")Integer status,
                                     @Param("key")String key);
    void batchPass(@Param("ids") List<Long> ids);
    void batchRefuse(@Param("ids") List<Long> ids);
    IPage<MusicianListVO> getMusician(@Param("page") Page<MusicianListVO> page,
                                      @Param("key")String key,
                                      @Param("status")Integer status);

}
