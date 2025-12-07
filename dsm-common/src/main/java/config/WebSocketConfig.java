package config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {

    @Bean // 将方法的返回值 扔到IOC容器中
    // 注入ServerEndpointExporter ，启动的时候会自动注册使用了@ServerEndpoint注解的bean
    public ServerEndpointExporter severEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
