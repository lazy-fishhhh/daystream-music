package social.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket 端点兼容类
 * 作为兼容层，保持与原有代码的接口一致
 * 内部调用 ChatWebSocketHandler 来发送消息
 */
@Slf4j
@Component
public class ChatEndPoint {

    /**
     * 向指定用户发送消息
     * @param userId 用户ID
     * @param message 消息内容
     */
    public static void sendMessageToUser(Long userId, String message) {
        try {
            ChatWebSocketHandler.sendMessageToUser(userId, message);
        } catch (Exception e) {
            log.error("向用户发送消息失败，userId: {}", userId, e);
        }
    }

    /**
     * 向多个用户发送消息
     * @param userIds 用户ID列表
     * @param message 消息内容
     */
    public static void sendMessageToUsers(List<Long> userIds, String message) {
        try {
            ChatWebSocketHandler.sendMessageToUsers(userIds, message);
        } catch (Exception e) {
            log.error("向多个用户发送消息失败", e);
        }
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public static boolean isUserOnline(Long userId) {
        return ChatWebSocketHandler.isUserOnline(userId);
    }
}
