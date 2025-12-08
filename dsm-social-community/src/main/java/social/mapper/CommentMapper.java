package social.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.Comment;
import org.apache.ibatis.annotations.*;
import social.domain.vo.CommentListVO;
import social.domain.vo.ReplyCommentListVO;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Update("update comment set like_count=like_count+1 where id=#{targetId}")
    void updateLikeCountup(Long targetId);

    @Update("update comment set like_count=like_count-1 where id=#{targetId} and like_count>0")
    void updateLikeCountLow(Long targetId);
    @Select("SELECT like_count FROM comment WHERE id = #{targetId} FOR UPDATE")
    Long selectCount(Long targetId);
    List<CommentListVO>commentList(@Param("targetId")Long targetId,
                                   @Param("lastId")Long lastId,
                                   @Param("size")Integer size,
                                   @Param("currentUserId")Long currentUserId);
    List<ReplyCommentListVO> replyCommentList(@Param("targetId")Long targetId,
                                              @Param("lastId")Long lastId,
                                              @Param("size")Integer size);
    Long hasmore(@Param("targetId")Long targetId, @Param("lastId")Long lastId);
    Long hasmoreparent(@Param("targetId")Long targetId, @Param("lastId")Long lastId);
    CommentListVO commentListvo(@Param("targetId")Long targetId);
    ReplyCommentListVO ReplyCommentVO(@Param("targetId")Long targetId);
    //更新回复数
    @Update("update comment set reply_count=reply_count+1 where id=#{targetId}")
    void updateReplyCountup(Long targetId);

    @Update("update comment set reply_count=reply_count-1 where id=#{targetId} and reply_count>0")
    void updateReplyCountLow(Long targetId);
    @Select("SELECT reply_count FROM comment WHERE id = #{targetId} FOR UPDATE")
    Long selectReplyCount(Long targetId);
/**
 * 获取需要删除的ID列表
 *
 * @param targetId 目标ID
 * @return 返回需要删除的ID列表，类型为Long的列表
 */
    List<Long> getDeleteId(Long targetId);
    Long deleteComment(@Param("ids")List<Long> ids,
                       @Param("targetId")Long targetId);
    void updateCommentCountByType(@Param("targetId")Long targetId,
                                  @Param("type")Integer type,
                                  @Param("count")Long count);
    Long selectCommentCount(@Param("type")Integer type,
                            @Param("targetId")Long targetId);
    void ParentCommentCount(@Param("targetId")Long targetId,
                            @Param("count")Long  count);
    Long selectTargetId(@Param("targetId")Long targetId,
                        @Param("type")Integer type,
                        @Param("userId")Long userId);
}
