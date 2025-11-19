package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.Playlist;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import social.domain.vo.CollectonLIstVO;


public interface PlayList2Mapper extends BaseMapper<Playlist> {
    @Update("update playlist set collect_count=collect_count+1 where id=#{targetId}")
    void playlistCollectCountup(Long targetId);

    @Update("update playlist set collect_count=collect_count-1 where id=#{targetId} and collect_count>0")
    void playlistCollectCountLow(Long targetId);
    @Select("SELECT collect_count FROM playlist WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);

    CollectonLIstVO selectLikeMusicVO(Long targetId);
}
