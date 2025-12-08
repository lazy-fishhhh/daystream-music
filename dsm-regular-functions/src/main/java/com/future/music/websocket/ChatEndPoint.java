package com.future.music.websocket;

import com.alibaba.fastjson2.JSON;
import com.future.login.util.SecurityUtil;
import entity.pojo.util.WebSocketMessage;
import entity.pojo.util.WebSocketMessageUtils;
import io.jsonwebtoken.Claims;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/*
 * 前端发送常规的HTTP请求，并在请求头中携带Upgrade:websocket(将http请求升级为websocket)
 *
 * 用户登录成功后，直接发起 WebSocket 连接，用户id立即被添加
 */
@ServerEndpoint("/chat")
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEndPoint {
    private  final StringRedisTemplate stringRedisTemplate;

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
    public void onOpen(Session session) throws EncodeException, IOException {
        // 鉴权
        Map<String, List<String>> params = session.getRequestParameterMap();
        String token = params.get("token") != null ? params.get("token").get(0) : null;

        Claims claims = JwtUtil.parseJWT(token);
        Long userId = claims.getSubject() != null ? Long.parseLong(claims.getId()) : null;
        if (userId == null) {
            // 鉴权失败，关闭连接
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token无效/未登录"));
            log.warn("token中的userId为null,无法建立WebSocket连接");
            throw new RuntimeException("token无效,无法建立WebSocket连接");
        }

        String redisTokenKey = TOKEN_USER_PREFIX + userId;
        String redisToken = stringRedisTemplate.opsForValue().get(redisTokenKey);

        // 检查Redis中的token是否存在且与请求中的token一致
        if (!StringUtils.hasText(redisToken) || !redisToken.equals(token)) {
            log.warn("未查询到redis中该token的缓存信息,无法建立WebSocket连接");
            throw new RuntimeException("token无效,无法建立WebSocket连接");
        }

        // 存储Session和初始心跳时间
        onlineUsers.put(userId, session);
        userLastHeartbeat.put(userId, System.currentTimeMillis());
        // 广播上线消息
        broadcastOnlineStatus();
    }

    /**
     * 接收客户端消息（包含心跳包）
     */
    @OnMessage
    public void onMessage(String message) throws EncodeException, IOException {
        Long userId = SecurityUtil.getCurrentUserId();
        // 解析消息
        WebSocketMessage msg = JSON.parseObject(message, WebSocketMessage.class);

        // 1. 心跳包处理（约定：message为"ping"表示心跳）
        if (PING.equals(msg.getMessage())) {
            userLastHeartbeat.put(userId, System.currentTimeMillis());
            // 回复心跳响应
            String pong = WebSocketMessageUtils.getMessage(true, null, "pong");
            sessionGet(userId).getBasicRemote().sendObject(pong);
            return;
        }

        // 2. 普通消息处理
        Long toUser = msg.getToUser();
        String mess = msg.getMessage().toString();
        Session toSession = onlineUsers.get(toUser);
        if (toSession != null && toSession.isOpen()) {
            String result = WebSocketMessageUtils.getMessage(false, userId, mess);
            toSession.getBasicRemote().sendObject(result);
        }
    }

    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(Session session) throws EncodeException, IOException {
        Long userId = SecurityUtil.getCurrentUserId();
        offlineProcess(userId);
    }

    /**
     * 连接异常时调用
     */
    @OnError
    public void onError(Session session, Throwable throwable) throws EncodeException, IOException {
        Long userId = SecurityUtil.getCurrentUserId();
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
        // ConcurrentHashMap的keySet是线程安全的，直接判断存在性即可
        return onlineUsers.containsKey(userId);
    }




    // ————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————
    /**
     * 离线处理统一逻辑
     */
    private static void offlineProcess(Long userId) throws EncodeException, IOException {
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
    private static void broadcastOnlineStatus() throws EncodeException, IOException {
        Set<Long> onlineUserIds = onlineUsers.keySet();
        String message = WebSocketMessageUtils.getMessage(true, null, onlineUserIds);
        broadcastToAllUsers(message);
    }

    /**
     * 广播消息给所有在线用户
     */
    private static void broadcastToAllUsers(String message) throws EncodeException, IOException {
        for (Session session : onlineUsers.values()) {
            if (session.isOpen()) {
                session.getBasicRemote().sendObject(message);
            }
        }
    }

    /**
     * 获取用户Session（空值保护）
     */
    private Session sessionGet(Long userId) {
        return onlineUsers.getOrDefault(userId, null);
    }
}