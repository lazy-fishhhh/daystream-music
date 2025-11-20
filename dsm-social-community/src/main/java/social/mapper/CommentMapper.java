package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Update("update comment set like_count=like_count+1 where id=#{targetId}")
    void updateLikeCountup(Long targetId);

    @Update("update comment set like_count=like_count-1 where id=#{targetId} and like_count>0")
    void updateLikeCountLow(Long targetId);
    @Select("SELECT like_count FROM comment WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);
}
