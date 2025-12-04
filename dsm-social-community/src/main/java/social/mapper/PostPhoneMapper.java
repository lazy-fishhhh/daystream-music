package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.PostPhotos;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostPhoneMapper extends BaseMapper<PostPhotos> {
List<String> selectPhotes(List<Long> ids);
List<String> selectPhote(Long postId);

    void deleteByPostId(Long postId);
    void deleteByPhotoUrl(@Param("postId")Long postId,
                          @Param("postUrl")String postUrl);
}
