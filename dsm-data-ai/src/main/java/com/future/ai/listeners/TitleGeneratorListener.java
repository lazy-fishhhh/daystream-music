package com.future.ai.listeners;

import com.future.ai.config.RabbitMqConfig;
import com.future.ai.domain.dto.TitleGeneratorMessage;
import com.future.ai.domain.entity.AiChatSession;
import com.future.ai.mapper.AiChatSessionMapper;
import com.future.ai.service.TitleGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class TitleGeneratorListener {

    @Resource
    private TitleGeneratorService titleGeneratorService;

    @Resource
    private AiChatSessionMapper sessionMapper;

    @RabbitListener(queues = RabbitMqConfig.TITLE_GENERATOR_QUEUE)
    public void handleTitleGeneratorMessage(TitleGeneratorMessage message) {
        try {
            log.info("收到标题生成请求：sessionId={}, userQuestion={}", message.getSessionId(), message.getUserQuestion());
            
            // 1. 调用大模型生成标题
            String title = titleGeneratorService.generateTitle(message.getUserQuestion());
            log.info("生成标题：{}", title);
            
            // 2. 查询会话（确保会话存在且标题仍为"新会话"）
            AiChatSession session = sessionMapper.selectById(message.getSessionId());
            // 3. 更新会话标题
            session.setTitle(title);
            sessionMapper.updateById(session);
            log.info("更新会话标题成功：sessionId={}, title={}", message.getSessionId(), title);
        } catch (Exception e) {
            log.error("处理标题生成请求失败：sessionId={}", message.getSessionId(), e);
        }
    }
}
