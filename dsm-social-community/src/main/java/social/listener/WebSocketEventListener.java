package social.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;   // 连接成功事件
import org.springframework.web.socket.messaging.SessionDisconnectEvent; // 断开连接事件
import social.service.OnlineUserService;

@Slf4j          // 日志
@Component      // 交给Spring管理
@RequiredArgsConstructor
public class WebSocketEventListener {

    // 在线用户服务（上线、下线、状态管理）
    private final OnlineUserService onlineUserService;

    // ======================================
    // 【事件1】用户连接上 WebSocket
    // ======================================
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // 获取连接的请求头信息
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 从会话里拿到前端传过来的 userId
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

        // 获取WebSocket的会话ID
        String sessionId = headerAccessor.getSessionId();

        // 如果用户ID不为空 → 标记【用户上线】
        if (userId != null) {
            onlineUserService.userOnline(userId, sessionId);
        }
    }

    // ======================================
    // 【事件2】用户断开 WebSocket 连接
    // ======================================
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 拿到断开连接的用户ID
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

        // 用户断开 → 标记【用户下线】
        if (userId != null) {
            onlineUserService.userOffline(userId);
        }
    }
}