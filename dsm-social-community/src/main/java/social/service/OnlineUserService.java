package social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import social.domain.vo.OnlineStatusVO;

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
@RequiredArgsConstructor
public class OnlineUserService {

    // Redis 存储【所有在线用户ID】的集合 key
    private static final String REDIS_ONLINE_USERS_KEY = "chat:online:users";
    // Redis 存储【用户ID -> 会话ID】的 key 前缀
    private static final String REDIS_USER_SESSION_PREFIX = "chat:session:";

    // Redis 操作工具
    private final StringRedisTemplate stringRedisTemplate;
    // WebSocket 消息发送模板（用于推送在线用户列表）
    private final SimpMessageSendingOperations messagingTemplate;

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
                messagingTemplate.convertAndSend("/topic/online-status", vo);
                log.debug("增量广播在线状态成功，userId: {}, status: {}", userId, status);
            } catch (Exception e) {
                log.error("增量广播在线状态失败，userId: {}, status: {}", userId, status, e);
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
}
