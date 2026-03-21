package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
    
    @Select("SELECT * FROM private_message WHERE to_user = #{userId} AND delivered = 0 ORDER BY send_time ASC")
    List<PrivateMessage> selectUndeliveredMessages(@Param("userId") Long userId);
    
    @Update("<script>" +
            "UPDATE private_message SET delivered = 1 WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchMarkAsDelivered(@Param("ids") List<Long> ids);
    
    @Update("UPDATE private_message SET read_time = NOW() WHERE id = #{msgId} AND to_user = #{userId}")
    int markAsRead(@Param("msgId") Long msgId, @Param("userId") Long userId);
}
