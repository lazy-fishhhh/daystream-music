package social.service.impl;

import com.future.login.util.SecurityUtil;
import entity.pojo.social.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import social.domain.vo.NotificationVO;
import social.domain.vo.OtherUserVO;
import social.domain.vo.UnreadNotificationCountVO;
import social.mapper.NotificationMapper;
import social.mapper.User2Mapper;
import social.service.NotificationService;
import social.service.OnlineUserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 通知服务实现类
 * 提供消息通知相关的业务功能实现
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationMapper notificationMapper;
    private final User2Mapper user2Mapper;
    private final SimpMessageSendingOperations messagingTemplate;
    private final OnlineUserService onlineUserService;
    
    @Autowired
    public NotificationServiceImpl(NotificationMapper notificationMapper, 
                                 User2Mapper user2Mapper, 
                                 SimpMessageSendingOperations messagingTemplate, 
                                 @Lazy OnlineUserService onlineUserService) {
        this.notificationMapper = notificationMapper;
        this.user2Mapper = user2Mapper;
        this.messagingTemplate = messagingTemplate;
        this.onlineUserService = onlineUserService;
    }
    
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendLikeNotification(Long fromUserId, Long toUserId, Long targetId, Integer targetType, String targetName, String targetContent, String content) {
        if (fromUserId.equals(toUserId)) {
            return; // 不向自己发送通知
        }
        
        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setFromUserId(fromUserId);
        notification.setType(1); // 1-点赞
        notification.setContent(content);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setTargetName(targetName);
        notification.setTargetContent(targetContent);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        
        notificationMapper.insert(notification);
        
        // 异步推送通知
        asyncPushNotification(toUserId, notification);
        
        log.info("发送点赞通知成功，from: {}, to: {}, targetId: {}", fromUserId, toUserId, targetId);
    }
    
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCommentNotification(Long fromUserId, Long toUserId, Long targetId, Integer targetType, String targetName, String targetContent, String content) {
        if (fromUserId.equals(toUserId)) {
            return; // 不向自己发送通知
        }
        
        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setFromUserId(fromUserId);
        notification.setType(2); // 2-评论
        notification.setContent(content);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setTargetName(targetName);
        notification.setTargetContent(targetContent);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        
        notificationMapper.insert(notification);
        
        // 异步推送通知
        asyncPushNotification(toUserId, notification);
        
        log.info("发送评论通知成功，from: {}, to: {}, targetId: {}", fromUserId, toUserId, targetId);
    }
    
    /**
     * 发送粉丝通知
     * @param fromUserId 发送用户ID
     * @param toUserId 接收用户ID
     * @param content 通知内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendFollowNotification(Long fromUserId, Long toUserId, String content) {
        if (fromUserId.equals(toUserId)) {
            return; // 不向自己发送通知
        }
        
        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setFromUserId(fromUserId);
        notification.setType(3); // 3-粉丝
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        
        notificationMapper.insert(notification);
        
        // 异步推送通知
        asyncPushNotification(toUserId, notification);
        
        log.info("发送粉丝通知成功，from: {}, to: {}", fromUserId, toUserId);
    }
    
    /**
     * 获取用户的通知列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 通知列表
     */
    @Override
    public List<NotificationVO> getNotifications(Long userId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notification> notifications = notificationMapper.selectNotifications(userId, offset, pageSize);
        
        return convertToVOList(notifications);
    }
    
    @Override
    public List<NotificationVO> getLikeNotifications(Long userId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notification> notifications = notificationMapper.selectNotificationsByType(userId, 1, offset, pageSize);
        
        return convertToVOList(notifications);
    }
    
    @Override
    public List<NotificationVO> getCommentNotifications(Long userId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notification> notifications = notificationMapper.selectNotificationsByType(userId, 2, offset, pageSize);
        
        return convertToVOList(notifications);
    }
    
    @Override
    public List<NotificationVO> getFollowNotifications(Long userId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notification> notifications = notificationMapper.selectNotificationsByType(userId, 3, offset, pageSize);
        
        return convertToVOList(notifications);
    }
    
    /**
     * 获取用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    @Override
    public Integer getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }
    
    /**
     * 获取用户未读消息数量（按类型）
     * @param userId 用户ID
     * @param type 通知类型
     * @return 未读消息数量
     */
    @Override
    public Integer getUnreadCountByType(Long userId, Integer type) {
        return notificationMapper.countUnreadByType(userId, type);
    }
    
    /**
     * 标记通知为已读
     * @param id 通知ID
     */
    @Override
    public void markAsRead(Long id) {
        // 获取通知信息，以便知道用户ID和通知类型
        Notification notification = notificationMapper.selectById(id);
        if (notification != null) {
            notificationMapper.markAsRead(id);
            log.info("标记通知为已读，id: {}", id);
            
            // 推送最新未读通知数量
            pushUnreadCount(notification.getUserId());
        }
    }
    
    /**
     * 批量标记通知为已读
     * @param ids 通知ID列表
     */
    @Override
    public void markBatchAsRead(List<Long> ids) {
        if (!ids.isEmpty()) {
            // 获取第一条通知的用户ID
            Notification firstNotification = notificationMapper.selectById(ids.get(0));
            if (firstNotification != null) {
                Long userId = firstNotification.getUserId();
                notificationMapper.markBatchAsRead(ids);
                log.info("批量标记通知为已读，数量: {}", ids.size());
                
                // 推送最新未读通知数量
                pushUnreadCount(userId);
            }
        }
    }
    
    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     */
    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
        log.info("标记所有通知为已读，userId: {}", userId);
        
        // 推送最新未读通知数量
        pushUnreadCount(userId);
    }
    
    /**
     * 获取用户的未读通知
     * @param userId 用户ID
     * @return 未读通知列表
     */
    @Override
    public List<NotificationVO> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreateTime)
        );
        
        return convertToVOList(notifications);
    }
    
    /**
     * 推送未读通知数量
     * @param userId 用户ID
     */
    private void pushUnreadCount(Long userId) {
        ExecutorService executor = OnlineUserService.getMessagePushExecutor();
        executor.execute(() -> {
            try {
                if (onlineUserService.isUserOnline(userId)) {
                    // 计算最新未读通知数量
                    Integer likeUnread = getUnreadCountByType(userId, 1);
                    Integer commentUnread = getUnreadCountByType(userId, 2);
                    Integer followUnread = getUnreadCountByType(userId, 3);
                    Integer privateMessageUnread = onlineUserService.getPrivateMessageUnreadCount(userId);
                    
                    UnreadNotificationCountVO unreadCount = new UnreadNotificationCountVO(
                        likeUnread, commentUnread, followUnread, privateMessageUnread
                    );
                    
                    // 推送未读通知数量
                    messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notification/unread-count",
                        unreadCount
                    );
                    log.debug("推送未读通知数量成功，userId: {}, 点赞: {}, 评论: {}, 粉丝: {}, 私信: {}", 
                        userId, likeUnread, commentUnread, followUnread, privateMessageUnread);
                }
            } catch (Exception e) {
                log.error("推送未读通知数量失败，userId: {}", userId, e);
            }
        });
    }
    
    /**
     * 异步推送通知
     * @param userId 用户ID
     * @param notification 通知对象
     */
    private void asyncPushNotification(Long userId, Notification notification) {
        ExecutorService executor = OnlineUserService.getMessagePushExecutor();
        executor.execute(() -> {
            try {

                // 在线直接推送通知
                // 离线的话 就上线获取最新未读通知数量
                if (onlineUserService.isUserOnline(userId)) {
                    // 1. 推送通知内容
                    NotificationVO vo = convertToVO(notification);
                    messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notification",
                        vo
                    );
                    log.debug("推送通知成功，userId: {}, type: {}", userId, notification.getType());
                    
                    // 2. 推送最新未读通知数量
                    Integer likeUnread = getUnreadCountByType(userId, 1);
                    Integer commentUnread = getUnreadCountByType(userId, 2);
                    Integer followUnread = getUnreadCountByType(userId, 3);
                    Integer privateMessageUnread = onlineUserService.getPrivateMessageUnreadCount(userId);
                    
                    UnreadNotificationCountVO unreadCount = new UnreadNotificationCountVO(
                        likeUnread, commentUnread, followUnread, privateMessageUnread
                    );
                    
                    messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notification/unread-count",
                        unreadCount
                    );
                    log.debug("推送未读通知数量成功，userId: {}, 点赞: {}, 评论: {}, 粉丝: {}, 私信: {}", 
                        userId, likeUnread, commentUnread, followUnread, privateMessageUnread);
                }
            } catch (Exception e) {
                log.error("推送通知失败，userId: {}", userId, e);
            }
        });
    }
    
    /**
     * 将通知实体转换为VO
     * @param notification 通知实体
     * @return 通知VO
     */
    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);
        
        // 获取发送用户信息
        OtherUserVO userInfo = user2Mapper.otherUserInfo(notification.getFromUserId());
        if (userInfo != null) {
            vo.setFromUserName(userInfo.getUsername());
            vo.setFromUserAvatar(userInfo.getAvatar());
        }
        
        return vo;
    }
    
    /**
     * 将通知实体列表转换为VO列表
     * @param notifications 通知实体列表
     * @return 通知VO列表
     */
    private List<NotificationVO> convertToVOList(List<Notification> notifications) {
        List<NotificationVO> voList = new ArrayList<>();
        for (Notification notification : notifications) {
            voList.add(convertToVO(notification));
        }
        return voList;
    }
}
