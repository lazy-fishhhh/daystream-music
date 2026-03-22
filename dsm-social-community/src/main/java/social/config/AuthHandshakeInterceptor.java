package social.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
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
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest.getServletRequest().getSession();
            
            String token = servletRequest.getServletRequest().getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            if (token == null) {
                token = servletRequest.getServletRequest().getParameter("token");
            }
            
            if (token != null) {
                Claims claims = JwtUtil.parseJWT(token);
                Long userId = Long.parseLong(claims.getId());
                attributes.put("userId", userId);
                log.info("WebSocket 握手成功，userId: {}", userId);
                return true;
            }
            
            log.warn("WebSocket 握手失败，未提供 token");
            return false;
        } catch (Exception e) {
            log.error("WebSocket 握手异常", e);
            return false;
        }
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                              WebSocketHandler wsHandler, Exception exception) {
    }
}
