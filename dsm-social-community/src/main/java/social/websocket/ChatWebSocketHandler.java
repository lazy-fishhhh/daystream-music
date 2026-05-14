package social.websocket;

import com.alibaba.fastjson2.JSON;
import entity.pojo.util.WebSocketMessage;
import entity.pojo.util.WebSocketMessageUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import util.JwtUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static constants.RedisCacheConstants.TOKEN_USER_PREFIX;

/**
 * Spring 原生 WebSocketHandler 实现
 * 处理 WebSocket 连接、消息和断开
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String PING = "ping";
    private static final String PONG = "pong";

    // 存储在线用户：用户ID -> Session
    private static final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
    // 存储用户最后心跳时间：用户ID -> 最后心跳时间戳(ms)
    private static final Map<Long, Long> userLastHeartbeat = new ConcurrentHashMap<>();
    // 心跳超时时间（30秒）
    private static final long HEARTBEAT_TIMEOUT = 30 * 1000;
    // 心跳检测定时任务
    private static final ScheduledExecutorService heartbeatChecker = Executors.newSingleThreadScheduledExecutor();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 静态初始化：启动心跳检测定时任务（每10秒扫描一次）
    static {
        heartbeatChecker.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            // 遍历检测超时连接
            for (Map.Entry<Long, Long> entry : userLastHeartbeat.entrySet()) {
                Long userId = entry.getKey();
                Long lastTime = entry.getValue();
                // 超时判定
                if (now - lastTime > HEARTBEAT_TIMEOUT) {
                    try {
                        // 清理连接
                        offlineProcess(userId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // ====================== 【关键日志】连接刚到达服务器，第一行打印 ======================
        log.info("=== WebSocket 连接刚到达服务器，SessionID：{} ===", session.getId());

        String token = null;
        // 从 URI 查询参数中获取 token
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            token = query.split("token=")[1].split("&")[0];
        }
        // 打印获取到的token，方便排查
        log.info("WebSocket 连接解析到token：{}", token);

        // 鉴权校验
        if (token == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("token无效/未登录"));
            log.warn("未提供token,无法建立WebSocket连接");
            return;
        }

        try {
            Claims claims = JwtUtil.parseJWT(token);
            Long userId = claims.getSubject() != null ? Long.parseLong(claims.getSubject()) : null;

            if (userId == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("token无效/未登录"));
                log.warn("token中的userId为null,无法建立WebSocket连接");
                return;
            }

            String redisTokenKey = TOKEN_USER_PREFIX + userId;
            
            if (stringRedisTemplate == null) {
                session.close(CloseStatus.SERVER_ERROR.withReason("服务依赖未就绪"));
                log.error("StringRedisTemplate 未初始化，拒绝 WebSocket 连接");
                return;
            }
            
            String redisToken = stringRedisTemplate.opsForValue().get(redisTokenKey);

            if (!StringUtils.hasText(redisToken) || !redisToken.equals(token)) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("token无效/已过期"));
                log.warn("未查询到redis中该token的缓存信息,无法建立WebSocket连接");
                return;
            }

            // 存储用户ID到Session
            session.getAttributes().put("userId", userId);
            // 存储Session和初始心跳时间
            onlineUsers.put(userId, session);
            userLastHeartbeat.put(userId, System.currentTimeMillis());
            // 广播上线消息
            broadcastOnlineStatus();

            log.info("WebSocket 连接成功 → userId:{}", userId);

        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("token解析失败"));
            log.error("WebSocket token 解析异常", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return;
        }
        
        // 解析消息
        WebSocketMessage msg = JSON.parseObject(message.getPayload(), WebSocketMessage.class);

        // 1. 心跳包处理（约定：message为"ping"表示心跳）
        if (PING.equals(msg.getMessage())) {
            userLastHeartbeat.put(userId, System.currentTimeMillis());
            // 回复心跳响应
            String pong = WebSocketMessageUtils.getMessage(true, null, "pong");
            session.sendMessage(new TextMessage(pong));
            return;
        }

        // 2. 普通消息处理
        Long toUser = msg.getToUser();
        String mess = msg.getMessage().toString();
        WebSocketSession toSession = onlineUsers.get(toUser);
        if (toSession != null && toSession.isOpen()) {
            String result = WebSocketMessageUtils.getMessage(false, userId, mess);
            toSession.sendMessage(new TextMessage(result));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        offlineProcess(userId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserIdFromSession(session);
        // 打印异常日志
        exception.printStackTrace();
        // 异常断开处理
        offlineProcess(userId);
    }

    /**
     * 从Session中获取用户ID
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }

    // 对外提供判断用户是否在线的静态方法
    public static boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        return onlineUsers.containsKey(userId);
    }

    /**
     * 离线处理统一逻辑
     */
    private static void offlineProcess(Long userId) throws IOException {
        if (userId == null) return;
        // 清理存储
        WebSocketSession session = onlineUsers.remove(userId);
        userLastHeartbeat.remove(userId);
        // 关闭会话
        if (session != null && session.isOpen()) {
            session.close();
        }
        // 广播离线状态
        broadcastOnlineStatus();
    }

    /**
     * 广播在线用户列表（统一状态同步）
     */
    private static void broadcastOnlineStatus() throws IOException {
        Set<Long> onlineUserIds = onlineUsers.keySet();
        String message = WebSocketMessageUtils.getMessage(true, null, onlineUserIds);
        broadcastToAllUsers(message);
    }

    /**
     * 广播消息给所有在线用户
     */
    private static void broadcastToAllUsers(String message) throws IOException {
        for (WebSocketSession session : onlineUsers.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    /**
     * 向指定用户发送消息
     * @param userId 用户ID
     * @param message 消息内容
     */
    public static void sendMessageToUser(Long userId, String message) {
        WebSocketSession session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("向用户发送消息成功，userId: {}", userId);
            } catch (IOException e) {
                log.error("向用户发送消息失败，userId: {}", userId, e);
            }
        }
    }

    /**
     * 向多个用户发送消息
     * @param userIds 用户ID列表
     * @param message 消息内容
     */
    public static void sendMessageToUsers(List<Long> userIds, String message) {
        for (Long userId : userIds) {
            sendMessageToUser(userId, message);
        }
    }
}
