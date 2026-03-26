package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import entity.pojo.social.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 消息通知Mapper
 * 用于消息通知的数据库操作
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    /**
     * 统计用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    default Integer countUnread(Long userId) {
        return Math.toIntExact(selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
        ));
    }
    
    /**
     * 统计用户未读消息数量（按类型）
     * @param userId 用户ID
     * @param type 通知类型
     * @return 未读消息数量
     */
    default Integer countUnreadByType(Long userId, Integer type) {
        return Math.toIntExact(selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getType, type)
                        .eq(Notification::getIsRead, 0)
        ));
    }
    
    /**
     * 获取用户的通知列表
     * @param userId 用户ID

     * @return 通知列表
     */
    List<Notification> selectNotifications(@Param("userId") Long userId, 
                                         @Param("offset") Integer offset, 
                                         @Param("limit") Integer limit);
    
    /**
     * 获取用户的通知列表（按类型）
     * @param userId 用户ID
     * @param type 通知类型
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 通知列表
     */
    List<Notification> selectNotificationsByType(@Param("userId") Long userId,
                                             @Param("type") Integer type,
                                             @Param("offset") Integer offset,
                                             @Param("limit") Integer limit);
    
    /**
     * 标记通知为已读
     * @param id 通知ID
     * @return 更新结果
     */
    @Update("UPDATE notification SET is_read = 1, read_time = NOW() WHERE id = #{id}")
    int markAsRead(Long id);
    
    /**
     * 批量标记通知为已读
     * @param ids 通知ID列表
     * @return 更新结果
     */
    int markBatchAsRead(@Param("ids") List<Long> ids);
    
    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @return 更新结果
     */
    @Update("UPDATE notification SET is_read = 1, read_time = NOW() WHERE user_id = #{userId}")
    int markAllAsRead(Long userId);
}
