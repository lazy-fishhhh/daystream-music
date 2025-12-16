package admin.mapper;

import admin.domain.vo.AdminPostVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.social.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface AdminPostMapper extends BaseMapper<CommunityPost> {
    IPage<AdminPostVO> communityList(@Param("page") Page<AdminPostVO> page,
                                     @Param("role")Integer role,
                                     @Param("key")String key);

    void updatePost(@Param("postId")Long postId,
                    @Param("status")Integer status,
                    @Param("content")String content,
                    @Param("title")String title);
    AdminPostVO detailPost(Long postId);

    IPage<AdminPostVO> getList(Page<AdminPostVO> page);
    void updatePostStatus(@Param("postId")Long postId,
                         @Param("status")Integer status);
    void deletedPost(@Param("postId")Long postId,
                     @Param("status")Integer status);
}
