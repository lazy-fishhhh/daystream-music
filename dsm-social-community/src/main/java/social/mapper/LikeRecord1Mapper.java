package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.LikeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public interface LikeRecord1Mapper extends BaseMapper<LikeRecord> {

    /**
     * 根据用户ID查询点赞记录中target_type=1的target_id集合
     * @param userId 用户ID
     * @return 点赞的target_id集合
     */
    @Select("select target_id from like_record where user_id = #{userId} and target_type = 1")
    Set<Long> selectLikeSongsByUserId(Long userId);

    /**
     * 批量查询多个用户的点赞记录中target_type=1的target_id集合
     * @param userIds 用户ID列表
     * @return 列表中的每个Map包含用户ID和对应的点赞歌曲ID字符串
     */
    List<Map<String, Object>> selectLikeSongsByUserIds(@Param("userIds") List<Long> userIds);
}
