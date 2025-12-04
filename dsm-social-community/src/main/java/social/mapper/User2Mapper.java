package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import social.domain.vo.OtherUserVO;
import social.domain.vo.UserInfoVO;

import java.util.List;

@Mapper
public interface User2Mapper extends BaseMapper<User> {
    List<Integer> roleIds(Long userId);
    UserInfoVO userInfo(Long userId);
    OtherUserVO otherUserInfo(Long userId);
    String selectPassword(@Param("userId") Long userId, @Param("email") String email);
    String selectEmail(Long userId);
}
