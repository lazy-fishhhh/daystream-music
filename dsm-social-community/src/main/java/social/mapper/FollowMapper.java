package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
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
                                             @Param("pageSize") Integer pageSize,
                                             @Param("currentId")Long currentId);
  List<UserFollowerVO> selectFollowedUserBatchMuscian(@Param("userIds") List<Long> userIds,
                                             @Param("lastId") Long lastId,
                                             @Param("pageSize") Integer pageSize,
                                             @Param("currentId")Long currentId);
  List<UserFollowerVO> selectFollowedUserBatchNormal(@Param("userIds") List<Long> userIds,
                                                      @Param("lastId") Long lastId,
                                                      @Param("pageSize") Integer pageSize,
                                                     @Param("currentId")Long currentId);
  List<UserFollowerVO> selectFollowedAllMuscian(@Param("userIds") List<Long> userIds,
                                                @Param("currentId")Long currentId);
  List<UserFollowerVO> selectFollowedAllNormal(@Param("userIds") List<Long> userIds,
                                               @Param("currentId")Long currentId);
  List<UserFollowerVO> selectFollowAll(@Param("userIds")List<Long> userIds,
                                       @Param("currentId")Long currentId);
  List<Long> selectFollowerUserIds(Long userId);
  List<Long> selectFollowUserIdsnormal(Long userId);
  List<Long> selectFollowUserIdsMusician(Long userId);
  Long countHasMore(@Param("userId") Long userId,
                    @Param("lastId") Long lastId);
  Long fansCount(Long userId);
  Long followCount(Long userId);
  @Update("update user set follow_count = follow_count + 1 where id = #{userId}")
  void updateFollowCountUp(@Param("userId") Long userId);
  @Update("update user set follow_count = follow_count - 1 where id = #{userId} and follow_count > 0")
  void updateFollowCountDown(@Param("userId") Long userId);
  @Update("update user set fans_count = fans_count + 1 where id = #{userId}")
  void updateFansCountUp(@Param("userId") Long userId);
  @Update("update user set fans_count = fans_count - 1 where id = #{userId} and fans_count > 0")
  void updateFansCountDown(@Param("userId") Long userId);

}
