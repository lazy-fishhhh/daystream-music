package social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 在线用户管理服务
 * 功能：记录用户上下线、查询在线状态、全局广播在线用户列表
 */
@Slf4j          // 日志注解
@Service        // Spring 服务类
@RequiredArgsConstructor // 构造器注入依赖
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

        // 4. 向所有客户端广播最新的在线用户列表
        broadcastOnlineStatus();
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

        // 4. 广播最新在线列表
        broadcastOnlineStatus();
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
     * 向所有客户端【广播最新在线用户列表】
     * 前端可以实时展示谁在线
     */
    private void broadcastOnlineStatus() {
        Set<Long> onlineUsers = getOnlineUsers();
        messagingTemplate.convertAndSend("/topic/online-users", onlineUsers);
    }
}