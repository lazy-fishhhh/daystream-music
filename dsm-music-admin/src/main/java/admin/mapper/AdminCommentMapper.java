package admin.mapper;

import admin.domain.dto.AdminCommentDTO;
import admin.domain.vo.AdminCommentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.social.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminCommentMapper extends BaseMapper<Comment> {
    IPage<AdminCommentVO> getCommentList(@Param("page") Page<AdminCommentVO> page,
                                         @Param("type")Integer type,
                                         @Param("keyword")String keyword);
    AdminCommentVO getDetail(@Param("id")Long id);

    void deleteComment(@Param("id")Long id);

    int updateComment(@Param("id")Long id,@Param("content")String content);
}
