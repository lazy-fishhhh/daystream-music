package social.config;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import util.JwtUtil;

import java.util.Map;

@Slf4j
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            HttpHeaders headers = request.getHeaders();
            String token = null;

            // ====================== 正确获取请求头（getFirst直接拿字符串，无List） ======================
            // 从 Sec-WebSocket-Protocol 获取token
            token = headers.getFirst("Sec-WebSocket-Protocol");

            // ====================== 必须回写响应头！浏览器强制要求，不写直接断开连接 ======================
            if (token != null) {
                response.getHeaders().set("Sec-WebSocket-Protocol", token);
            }

            // 如果没有，从 Authorization 获取
            if (token == null) {
                token = headers.getFirst("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }

            // 校验token
            if (token != null) {
                Claims claims = JwtUtil.parseJWT(token);
                Long userId = Long.parseLong(claims.getSubject());
                attributes.put("userId", userId);
                log.info("WebSocket 握手成功，userId: {}", userId);
                // ====================== 必须回写响应头！浏览器强制要求，不写直接断开连接 ======================
                response.getHeaders().set("Sec-WebSocket-Protocol", token);
                return true;
            }

            log.warn("WebSocket 握手失败，未提供有效的token");
            return false;
        } catch (Exception e) {
            log.error("WebSocket 握手异常，token解析失败", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}