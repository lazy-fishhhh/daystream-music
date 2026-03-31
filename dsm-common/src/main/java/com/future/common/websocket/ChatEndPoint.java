package com.future.common.websocket;

import com.alibaba.fastjson2.JSON;
import entity.pojo.util.WebSocketMessage;
import entity.pojo.util.WebSocketMessageUtils;
import io.jsonwebtoken.Claims;
import jakarta.websocket.*;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

@ServerEndpoint(value = "/chat", configurator = ChatEndPoint.CustomConfigurator.class)
@Component

// WebSocket 是多实例，不能用构造器注入 Spring Bean，会导致空指针
// @RequiredArgsConstructor 会导致每个实例都有自己的依赖，而不是共享的
@Slf4j
public class ChatEndPoint {

    // 自定义配置器：清空无用逻辑，适配 URL 传 token
    public static class CustomConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {

        }
    }

    // ====================== StringRedisTemplate 改为 静态变量 ======================
    // 原因：WebSocket 多实例共享 Redis 工具，必须 static
    private static StringRedisTemplate stringRedisTemplate;

    // ====================== 使用 Setter 方法注入，替代构造器注入 ======================
    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        ChatEndPoint.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String PING = "ping";
    private static final String PONG = "pong";

    // 存储在线用户：用户ID -> Session
    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();
    // 存储用户最后心跳时间：用户ID -> 最后心跳时间戳(ms)
    private static final Map<Long, Long> userLastHeartbeat = new ConcurrentHashMap<>();
    // 心跳超时时间（30秒）
    private static final long HEARTBEAT_TIMEOUT = 30 * 1000;
    // 心跳检测定时任务
    private static final ScheduledExecutorService heartbeatChecker = Executors.newSingleThreadScheduledExecutor();

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

    /**
     * 建立websocket连接后调用
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws IOException {

        String token = null;
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params.containsKey("token")) {
            token = params.get("token").get(0);
        }

        // 鉴权校验
        if (token == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token无效/未登录"));
            log.warn("未提供token,无法建立WebSocket连接");
            return;
        }

        try {
            Claims claims = JwtUtil.parseJWT(token);
            Long userId = claims.getSubject() != null ? Long.parseLong(claims.getSubject()) : null;

            if (userId == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token无效/未登录"));
                log.warn("token中的userId为null,无法建立WebSocket连接");
                return;
            }

            String redisTokenKey = TOKEN_USER_PREFIX + userId;
            String redisToken = stringRedisTemplate.opsForValue().get(redisTokenKey);

            if (!StringUtils.hasText(redisToken) || !redisToken.equals(token)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token无效/已过期"));
                log.warn("未查询到redis中该token的缓存信息,无法建立WebSocket连接");
                return;
            }

            // 存储用户ID到Session
            session.getUserProperties().put("userId", userId);
            // 存储Session和初始心跳时间
            onlineUsers.put(userId, session);
            userLastHeartbeat.put(userId, System.currentTimeMillis());
            // 广播上线消息
            broadcastOnlineStatus();

            log.info("WebSocket 连接成功 → userId:{}", userId);

        } catch (Exception e) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token解析失败"));
            log.error("WebSocket token 解析异常", e);
        }
    }

    /**
     * 从Session中获取用户ID
     */
    private Long getUserIdFromSession(Session session) {
        return (Long) session.getUserProperties().get("userId");
    }

    /**
     * 接收客户端消息（包含心跳包）
     */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        Long userId = getUserIdFromSession(session);
        // 解析消息
        WebSocketMessage msg = JSON.parseObject(message, WebSocketMessage.class);

        // 1. 心跳包处理（约定：message为"ping"表示心跳）
        if (PING.equals(msg.getMessage())) {
            userLastHeartbeat.put(userId, System.currentTimeMillis());
            // 回复心跳响应
            String pong = WebSocketMessageUtils.getMessage(true, null, "pong");
            session.getBasicRemote().sendText(pong);
            return;
        }

        // 2. 普通消息处理
        Long toUser = msg.getToUser();
        String mess = msg.getMessage().toString();
        Session toSession = onlineUsers.get(toUser);
        if (toSession != null && toSession.isOpen()) {
            String result = WebSocketMessageUtils.getMessage(false, userId, mess);
            toSession.getBasicRemote().sendText(result);
        }
    }

    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        Long userId = getUserIdFromSession(session);
        offlineProcess(userId);
    }

    /**
     * 连接异常时调用
     */
    @OnError
    public void onError(Session session, Throwable throwable) throws IOException {
        Long userId = getUserIdFromSession(session);
        // 打印异常日志
        throwable.printStackTrace();
        // 异常断开处理
        offlineProcess(userId);
    }

    // 对外提供判断用户是否在线的静态方法
    public static boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        return onlineUsers.containsKey(userId);
    }

    // ————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
    /**
     * 离线处理统一逻辑
     */
    private static void offlineProcess(Long userId) throws IOException {
        if (userId == null) return;
        // 清理存储
        onlineUsers.remove(userId);
        userLastHeartbeat.remove(userId);
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
        for (Session session : onlineUsers.values()) {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        }
    }

    /**
     * 向指定用户发送消息
     * @param userId 用户ID
     * @param message 消息内容
     */
    public static void sendMessageToUser(Long userId, String message) {
        Session session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
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