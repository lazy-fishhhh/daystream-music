package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.Playlist;
import net.bytebuddy.description.field.FieldDescription;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import social.domain.vo.CollectonLIstVO;

import java.util.List;


public interface PlayList2Mapper extends BaseMapper<Playlist> {
    @Update("update playlist set collect_count=collect_count+1 where id=#{targetId}")
    void playlistCollectCountup(Long targetId);

    @Update("update playlist set collect_count=collect_count-1 where id=#{targetId} and collect_count>0")
    void playlistCollectCountLow(Long targetId);
    @Select("SELECT collect_count FROM playlist WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);

    CollectonLIstVO selectPlayListVO(@Param("targetId") Long targetId,
                                      @Param("userId")Long userId);
    List<CollectonLIstVO> selectPlayListBatchIds(@Param("userId")Long userId,
                                                 @Param("lastId")Long lastId,
                                                 @Param("pageSize")Integer pageSize);

    List<CollectonLIstVO> selectPlayListAll(Long userId);
    @Update("update playlist set comment_count=comment_count+1 where id=#{targetId}")
    void playlistCommentCountup(Long targetId);

    @Update("update playlist set comment_count=comment_count-1 where id=#{targetId} and comment_count>0")
    void playlistCommentCountLow(Long targetId);
    @Select("SELECT comment_count FROM playlist WHERE id = #{targetId} FOR UPDATE")
    Long selectCommentCount(Long targetId);

}
