package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import social.domain.vo.CommunityVO;
import social.domain.vo.PostDetailVO;

import java.util.List;

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
    //更新点赞数量
    @Update("update comment set like_count=like_count+1 where id=#{targetId}")
    void updateLikeCountup(Long targetId);

    @Update("update comment set like_count=like_count-1 where id=#{targetId} and like_count>0")
    void updateLikeCountLow(Long targetId);
    @Select("SELECT like_count FROM comment WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);
    List<CommunityVO> selectCommunityPost(@Param("userId")Long userId,
                                          @Param("pageSize")Integer pageSize,
                                          @Param("lastId")Long lastId,
                                          @Param("key")String key);

    Boolean hasmore(@Param("userId")Long userId,
                    @Param("lastId")Long lastId,
                    @Param("key")String key);
    PostDetailVO selectDetail(Long postId);
    boolean isLike(@Param("userId")Long userId,
                   @Param("postId")Long postId);
    boolean isFollow(@Param("userId")Long userId,
                     @Param("authorId")Long authorId);
}
