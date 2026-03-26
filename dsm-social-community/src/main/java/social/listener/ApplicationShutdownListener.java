package social.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import social.service.OnlineUserService;

/**
 * 应用关闭监听器
 * 在应用关闭时优雅地关闭线程池
 */
@Slf4j
@Component
public class ApplicationShutdownListener implements ApplicationListener<ContextClosedEvent> {
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("应用正在关闭，开始清理资源...");
        
        try {
            OnlineUserService.shutdownExecutor();
            log.info("消息推送线程池已关闭");
        } catch (Exception e) {
            log.error("关闭线程池失败", e);
        }
    }
}
