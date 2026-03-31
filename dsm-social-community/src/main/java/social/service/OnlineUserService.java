package social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import websocket.ChatEndPoint;
import entity.pojo.util.WebSocketMessageUtils;
import entity.pojo.social.GroupMessage;
import entity.pojo.social.PrivateMessage;
import social.domain.vo.OfflineMessageSummaryVO;
import social.domain.vo.OnlineStatusVO;
import social.domain.vo.UnreadNotificationCountVO;
import social.service.ChatService;
import social.service.NotificationService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 在线用户管理服务
 * 功能：记录用户上下线、查询在线状态、增量广播在线状态变更
 */
@Slf4j
@Service
public class OnlineUserService {

    // Redis 存储【所有在线用户ID】的集合 key
    private static final String REDIS_ONLINE_USERS_KEY = "chat:online:users";
    // Redis 存储【用户ID -> 会话ID】的 key 前缀
    private static final String REDIS_USER_SESSION_PREFIX = "chat:session:";

    // Redis 操作工具
    private final StringRedisTemplate stringRedisTemplate;
    
    // 聊天服务，用于获取离线消息
    private final ChatService chatService;
    
    private final NotificationService notificationService;
    
    @Autowired
    public OnlineUserService(StringRedisTemplate stringRedisTemplate, 
                            @Lazy ChatService chatService,
                            @Lazy NotificationService notificationService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.chatService = chatService;
        this.notificationService = notificationService;
    }

    // 本地缓存：用户ID -> WebSocket 会话ID，高并发安全
    private final ConcurrentHashMap<Long, String> userSessionMap = new ConcurrentHashMap<>();
    
    // 异步推送线程池
    private static final ExecutorService MESSAGE_PUSH_EXECUTOR = new ThreadPoolExecutor(
        5,                      // 核心线程数
        20,                     // 最大线程数
        60L,                    // 空闲线程存活时间
        TimeUnit.SECONDS,        // 时间单位
        new LinkedBlockingQueue<>(1000),  // 任务队列
        new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：调用者线程执行
    );

    /**
     * 用户上线
     * @param userId 用户ID
     * @param sessionId WebSocket 会话ID
     */
    public void userOnline(Long userId, String sessionId) {
        // 1. 存入本地缓存
        userSessionMap.put(userId, sessionId);
        // 2. Redis 集合记录在线用户
        stringRedisTemplate.opsForSet().add(REDIS_ONLINE_USERS_KEY, userId.toString());
        // 3. Redis 记录用户对应的会话ID
        stringRedisTemplate.opsForValue().set(REDIS_USER_SESSION_PREFIX + userId, sessionId);
        log.info("用户上线，userId: {}, sessionId: {}", userId, sessionId);

        // 4. 异步增量广播上线状态
        asyncBroadcastOnlineStatus(userId, 1);
        
        // 5. 异步获取并推送离线消息
        asyncPushOfflineMessages(userId);
    }

    /**
     * 用户下线
     * @param userId 用户ID
     */
    public void userOffline(Long userId) {
        // 1. 从本地缓存移除
        userSessionMap.remove(userId);
        // 2. 从 Redis 在线集合移除
        stringRedisTemplate.opsForSet().remove(REDIS_ONLINE_USERS_KEY, userId.toString());
        // 3. 删除 Redis 中该用户的会话记录
        stringRedisTemplate.delete(REDIS_USER_SESSION_PREFIX + userId);
        log.info("用户下线，userId: {}", userId);

        // 4. 异步增量广播下线状态
        asyncBroadcastOnlineStatus(userId, 0);
    }

    /**
     * 判断用户是否在线
     * 先查本地缓存，缓存没有再查 Redis
     * @param userId 用户ID
     * @return 在线返回true
     */
    public boolean isUserOnline(Long userId) {
        return userSessionMap.containsKey(userId) ||
                Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(REDIS_ONLINE_USERS_KEY, userId.toString()));
    }

    /**
     * 获取所有在线用户ID集合
     * @return 在线用户ID Set
     */
    public Set<Long> getOnlineUsers() {
        Set<String> onlineUserIds = stringRedisTemplate.opsForSet().members(REDIS_ONLINE_USERS_KEY);
        return onlineUserIds != null ?
                onlineUserIds.stream().map(Long::parseLong).collect(Collectors.toSet()) :
                java.util.Collections.emptySet();
    }
    
    /**
     * 异步增量广播在线状态变更
     * @param userId 用户ID
     * @param status 状态：0-离线，1-在线
     */
    private void asyncBroadcastOnlineStatus(Long userId, Integer status) {
        MESSAGE_PUSH_EXECUTOR.execute(() -> {
            try {
                OnlineStatusVO vo = new OnlineStatusVO(userId, status, System.currentTimeMillis());
                // 广播在线状态变更给所有在线用户
                String message = WebSocketMessageUtils.getMessage(true, null, vo);
                Set<Long> onlineUsers = getOnlineUsers();
                for (Long onlineUserId : onlineUsers) {
                    ChatEndPoint.sendMessageToUser(onlineUserId, message);
                }
                log.debug("增量广播在线状态成功，userId: {}, status: {}", userId, status);
            } catch (Exception e) {
                log.error("增量广播在线状态失败，userId: {}, status: {}", userId, status, e);
            }
        });
    }
    
    /**
     * 异步获取并推送离线消息
     * @param userId 用户ID
     */
    private void asyncPushOfflineMessages(Long userId) {
        MESSAGE_PUSH_EXECUTOR.execute(() -> {
            try {
                log.info("开始获取用户离线消息，userId: {}", userId);
                
                // 1. 推送未读通知数量
                Integer likeUnread = notificationService.getUnreadCountByType(userId, 1);
                Integer commentUnread = notificationService.getUnreadCountByType(userId, 2);
                Integer followUnread = notificationService.getUnreadCountByType(userId, 3);
                Integer privateMessageUnread = chatService.getPrivateMessageUnreadCount(userId);
                
                UnreadNotificationCountVO unreadCount = new UnreadNotificationCountVO(
                    likeUnread, commentUnread, followUnread, privateMessageUnread
                );
                
                String unreadCountMessage = WebSocketMessageUtils.getMessage(true, null, unreadCount);
                ChatEndPoint.sendMessageToUser(userId, unreadCountMessage);
                log.info("推送未读通知数量成功，userId: {}, 点赞: {}, 评论: {}, 粉丝: {}, 私信: {}", 
                    userId, likeUnread, commentUnread, followUnread, privateMessageUnread);
                
                // 2. 推送未读通知
                List<social.domain.vo.NotificationVO> unreadNotifications = notificationService.getUnreadNotifications(userId);
                if (!unreadNotifications.isEmpty()) {
                    String notificationMessage = WebSocketMessageUtils.getMessage(true, null, unreadNotifications);
                    ChatEndPoint.sendMessageToUser(userId, notificationMessage);
                    log.info("推送未读通知成功，userId: {}, 通知数: {}", userId, unreadNotifications.size());
                }
                
                // 3. 推送离线消息摘要
                List<OfflineMessageSummaryVO> offlineSummary = chatService.getOfflineMessageSummary(userId);
                if (!offlineSummary.isEmpty()) {
                    String summaryMessage = WebSocketMessageUtils.getMessage(true, null, offlineSummary);
                    ChatEndPoint.sendMessageToUser(userId, summaryMessage);
                    log.info("推送离线消息摘要成功，userId: {}, 摘要数: {}", userId, offlineSummary.size());
                }

//                //这些不应该在上线时立刻推送,而是等用户点击离线消息时主动前端调用接口获取
//                // 2. 获取私聊离线消息
//                List<PrivateMessage> privateMessages = chatService.getOfflineMessages(userId);
//                if (!privateMessages.isEmpty()) {
//                    for (PrivateMessage message : privateMessages) {
//                        messagingTemplate.convertAndSendToUser(
//                            userId.toString(),
//                            "/queue/private",
//                            message
//                        );
//                    }
//                    log.info("推送私聊离线消息成功，userId: {}, 消息数: {}", userId, privateMessages.size());
//                }
//
//                // 3. 获取群聊离线消息
//                List<GroupMessage> groupMessages = chatService.getGroupOfflineMessages(userId);
//                if (!groupMessages.isEmpty()) {
//                    for (GroupMessage message : groupMessages) {
//                        messagingTemplate.convertAndSendToUser(
//                            userId.toString(),
//                            "/queue/group",
//                            message
//                        );
//                    }
//                    log.info("推送群聊离线消息成功，userId: {}, 消息数: {}", userId, groupMessages.size());
//                }
                
                log.info("离线消息处理完成，userId: {}", userId);
            } catch (Exception e) {
                log.error("获取并推送离线消息失败，userId: {}", userId, e);
            }
        });
    }
    
    /**
     * 获取消息推送线程池
     * @return 线程池
     */
    public static ExecutorService getMessagePushExecutor() {
        return MESSAGE_PUSH_EXECUTOR;
    }
    
    /**
     * 关闭线程池（应用关闭时调用）
     */
    public static void shutdownExecutor() {
        MESSAGE_PUSH_EXECUTOR.shutdown();
        try {
            if (!MESSAGE_PUSH_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                MESSAGE_PUSH_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            MESSAGE_PUSH_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取用户未读私信数量
     * @param userId 用户ID
     * @return 未读私信数量
     */
    public Integer getPrivateMessageUnreadCount(Long userId) {
        return chatService.getPrivateMessageUnreadCount(userId);
    }
}
