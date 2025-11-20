package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.Album;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import social.domain.vo.CollectioAlbumVO;

import java.util.List;

@Mapper
public interface Album2Mapper extends BaseMapper<Album> {
    @Update("update album set collect_count=collect_count+1 where id=#{targetId}")
    void albumCollectCountup(Long targetId);

    @Update("update album set collect_count=collect_count-1 where id=#{targetId} and collect_count>0")
    void albumCollectCountLow(Long targetId);
    @Select("SELECT collect_count FROM album WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);
    CollectioAlbumVO selectAlbumList(@Param("targetId") Long targetId,
                                     @Param("userId")Long userId);
    List<CollectioAlbumVO> selectAlbumListBatchIds(@Param("userId")Long userId,
                                                   @Param("pageSize")Integer pageSize,
                                                   @Param("lastId")Long lastId);
}
