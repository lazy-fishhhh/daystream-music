package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {
    
    @Select("SELECT * FROM group_message WHERE group_id = #{groupId} AND id > #{lastReadMsgId} ORDER BY send_time ASC")
    List<GroupMessage> selectUnreadMessages(@Param("groupId") Long groupId, @Param("lastReadMsgId") Long lastReadMsgId);
}
