package social.service;

import entity.pojo.social.Notification;
import social.domain.vo.NotificationVO;

import java.util.List;

/**
 * 通知服务接口
 * 提供消息通知相关的业务功能
 */
public interface NotificationService {
    
    /**
     * 发送点赞通知
     * @param fromUserId 发送用户ID
     * @param toUserId 接收用户ID
     * @param targetId 点赞对象ID
     * @param targetType 点赞对象类型
     * @param targetName 目标对象名称
     * @param targetContent 目标对象内容
     * @param content 通知内容
     */
    void sendLikeNotification(Long fromUserId, Long toUserId, Long targetId, Integer targetType, String targetName, String targetContent, String content);
    
    /**
     * 发送评论通知
     * @param fromUserId 发送用户ID
     * @param toUserId 接收用户ID
     * @param targetId 评论对象ID
     * @param targetType 评论对象类型
     * @param targetName 目标对象名称
     * @param targetContent 目标对象内容
     * @param content 通知内容
     */
    void sendCommentNotification(Long fromUserId, Long toUserId, Long targetId, Integer targetType, String targetName, String targetContent, String content);
    
    /**
     * 发送粉丝通知
     * @param fromUserId 发送用户ID
     * @param toUserId 接收用户ID
     * @param content 通知内容
     */
    void sendFollowNotification(Long fromUserId, Long toUserId, String content);
    
    /**
     * 获取用户的通知列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 通知列表
     */
    List<NotificationVO> getNotifications(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取用户点赞通知列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 点赞通知列表
     */
    List<NotificationVO> getLikeNotifications(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取用户评论通知列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 评论通知列表
     */
    List<NotificationVO> getCommentNotifications(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取用户粉丝通知列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 粉丝通知列表
     */
    List<NotificationVO> getFollowNotifications(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Integer getUnreadCount(Long userId);
    
    /**
     * 获取用户未读消息数量（按类型）
     * @param userId 用户ID
     * @param type 通知类型
     * @return 未读消息数量
     */
    Integer getUnreadCountByType(Long userId, Integer type);
    
    /**
     * 标记通知为已读
     * @param id 通知ID
     */
    void markAsRead(Long id);
    
    /**
     * 批量标记通知为已读
     * @param ids 通知ID列表
     */
    void markBatchAsRead(List<Long> ids);
    
    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);
    
    /**
     * 获取用户的未读通知
     * @param userId 用户ID
     * @return 未读通知列表
     */
    List<NotificationVO> getUnreadNotifications(Long userId);
}
