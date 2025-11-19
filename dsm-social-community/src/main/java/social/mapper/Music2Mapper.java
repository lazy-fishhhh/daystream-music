package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import social.domain.vo.LikeMusicVO;


@Mapper
public interface Music2Mapper extends BaseMapper<Music> {
    @Update("update music set like_count=like_count+1 where id=#{targetId}")
    void likeCountUp(Long targetId);

    @Update("update music set like_count=like_count-1 where id=#{targetId} and like_count>0")
    void likeCountLow(Long targetId);
    @Select("SELECT like_count FROM music WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);
    LikeMusicVO selectLikeMusicVO(Long targetId);

}
