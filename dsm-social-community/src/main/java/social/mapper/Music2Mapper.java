package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import social.domain.vo.LikeMusicVO;

import java.util.List;


@Mapper
public interface Music2Mapper extends BaseMapper<Music> {
    @Update("update music set like_count=like_count+1 where id=#{targetId}")

    void likeCountUp(Long targetId);
    @Update("update music set like_count=like_count-1 where id=#{targetId} and like_count>0")
    void likeCountLow(Long targetId);
    @Select("SELECT like_count FROM music WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);
    LikeMusicVO selectLikeMusicVO(@Param("targetId") Long targetId,
                                  @Param("userId")Long userId);
    List<LikeMusicVO> selectLikeMusicVOBatchIds(@Param("lastId")Long lastId,@Param("pageSize")Integer pageSize,@Param("userId")Long userId);
    List<LikeMusicVO> selectLikeMusicVOAll(Long userId);
    @Update("update music set comment_count=comment_count+1 where id=#{targetId}")

    void commentCountUp(Long targetId);
    @Update("update music set comment_count=comment_count-1 where id=#{targetId} and comment_count>0")
    void commentCountLow(Long targetId);
    @Select("SELECT comment_count FROM music WHERE id = #{targetId} FOR UPDATE")
    Long selectCommentCount(Long targetId);
    @Update("update music set comment_count=comment_count-count where id=#{targetId}")
    void musicCommentCountLowBatch(Long targetId,Long count);
}
