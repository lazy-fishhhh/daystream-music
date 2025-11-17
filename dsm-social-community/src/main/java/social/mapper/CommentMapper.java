package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
