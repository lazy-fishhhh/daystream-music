package social.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 【核心】开启WebSocket消息代理功能（开启聊天功能）
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthHandshakeInterceptor authHandshakeInterceptor;
    
    public WebSocketStompConfig(AuthHandshakeInterceptor authHandshakeInterceptor) {
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. 启用简单消息代理，设置【订阅推送】的通道前缀
        config.enableSimpleBroker("/topic", "/queue");

        // 2. 客户端【发送消息】的请求前缀
        config.setApplicationDestinationPrefixes("/app");

        // 3. 点对点私聊前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")       // 连接地址：ws://服务器IP:端口/ws/chat
                .addInterceptors(authHandshakeInterceptor) // 添加握手拦截器
                .setAllowedOriginPatterns("*") // 允许所有前端跨域访问
                .withSockJS();                 // 支持SockJS（兼容不支持WebSocket的浏览器）
    }
}
