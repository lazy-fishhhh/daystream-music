package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {
    //更新评论量
    @Update("update community_post set comment_count=comment_count+1 where id=#{targetId}")
    void communityCommentCountup(Long targetId);

    @Update("update community_post set comment_count=comment_count-1 where id=#{targetId} and comment_count>0")
    void communityCommentCountLow(Long targetId);
    @Select("SELECT comment_count FROM community_post WHERE id = #{targetId} FOR UPDATE")
    Long selectCommetCount(Long targetId);
    @Update("update community_post set comment_count=comment_count-count where id=#{targetId}")
    void communtiyCommentCountLowBatch(Long targetId,Long count);

}
