package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import social.domain.pojo.UserFollow;
import social.domain.vo.UserFollowerVO;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<UserFollow> {
  UserFollowerVO selectFollowUser(@Param("userId") Long userId,
                                  @Param("currentId")Long currentId);
  UserFollowerVO selectFollowedUser(@Param("userId") Long userId,
                                    @Param("currentId")Long currentId);
  List<UserFollowerVO> selectFollowUserBatch(@Param("userIds") List<Long> userIds,
                                             @Param("lastId") Long lastId,
                                             @Param("pageSize") Integer pageSize);
  List<UserFollowerVO> selectFollowedUserBatchMuscian(@Param("userIds") List<Long> userIds,
                                             @Param("lastId") Long lastId,
                                             @Param("pageSize") Integer pageSize);
  List<UserFollowerVO> selectFollowedUserBatchNormal(@Param("userIds") List<Long> userIds,
                                                      @Param("lastId") Long lastId,
                                                      @Param("pageSize") Integer pageSize);
  List<UserFollowerVO> selectFollowedAllMuscian(List<Long> userIds);
  List<UserFollowerVO> selectFollowedAllNormal(List<Long> userIds);
  List<UserFollowerVO> selectFollowAll(List<Long> userIds);
  List<Long> selectFollowerUserIds(Long userId);
  List<Long> selectFollowUserIdsnormal(Long userId);
  List<Long> selectFollowUserIdsMusician(Long userId);
  Long countHasMore(@Param("userId") Long userId,
                    @Param("lastId") Long lastId);

}
