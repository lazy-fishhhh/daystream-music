package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    
    @Select("SELECT * FROM group_member WHERE user_id = #{userId} AND status = 0")
    List<GroupMember> selectUserGroups(@Param("userId") Long userId);
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND status = 0")
    List<GroupMember> selectGroupMembers(@Param("groupId") Long groupId);
    
    @Update("UPDATE group_member SET last_read_msg_id = #{msgId} WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateLastReadMsgId(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("msgId") Long msgId);
}
