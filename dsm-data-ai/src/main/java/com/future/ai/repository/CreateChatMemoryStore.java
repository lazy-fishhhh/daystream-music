package com.future.ai.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.future.ai.config.RabbitMqConfig;
import com.future.ai.domain.dto.TitleGeneratorMessage;
import com.future.ai.domain.entity.AiChatMessage;
import com.future.ai.domain.entity.AiChatSession;
import com.future.ai.mapper.AiChatMessageMapper;
import com.future.ai.mapper.AiChatSessionMapper;
import com.future.ai.service.TitleGeneratorService;
import com.future.login.util.SecurityUtil;
import constants.RedisCacheConstants;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class CreateChatMemoryStore implements ChatMemoryStore {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AiChatSessionMapper sessionMapper;

    @Resource
    private AiChatMessageMapper messageMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = RedisCacheConstants.AI_CREATE + memoryId;

        // 先从Redis获取，未命中则从MySQL加载并回写Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            List<ChatMessage> messages = loadMessagesFromMysql(memoryId);
            if (!messages.isEmpty()) {
                stringRedisTemplate.opsForValue().set(key,
                        ChatMessageSerializer.messagesToJson(messages), Duration.ofDays(1));
            }
            return messages;
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }
    // 从MySQL加载历史消息
    private List<ChatMessage> loadMessagesFromMysql(Object memoryId) {
        Long sessionId = Long.valueOf(memoryId.toString());
        QueryWrapper<AiChatMessage> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByAsc("send_time");
        List<AiChatMessage> dbMessages = messageMapper.selectList(query);

        // 转换为ChatMessage列表
        return dbMessages.stream().map(msg -> {
            if (msg.getMessageType() == 1) {
                return UserMessage.userMessage(msg.getContent());
            } else {
                return AiMessage.aiMessage(msg.getContent());
            }
        }).collect(Collectors.toList());
    }


    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Long sessionId = Long.valueOf(memoryId.toString());
        // 确保会话记录存在
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            AiChatSession newSession = new AiChatSession();
            newSession.setId(sessionId);
            newSession.setUserId(SecurityUtil.getCurrentUserId()); // 从SecurityContext获取当前用户ID
            newSession.setSessionType(2); // 1=咨询，2=音乐创作

            sessionMapper.insert(newSession);
        }else {
            if (Objects.equals(session.getTitle(), "新会话")){
                for (ChatMessage message : messages) {
                    if (message instanceof UserMessage) {
                        String userQuestion = ((UserMessage) message).singleText();
                        // 使用 RabbitMQ 发送标题生成请求
                        TitleGeneratorMessage titleMessage = new TitleGeneratorMessage();
                        titleMessage.setSessionId(sessionId);
                        titleMessage.setUserQuestion(userQuestion);
                        rabbitTemplate.convertAndSend(
                                RabbitMqConfig.TITLE_GENERATOR_EXCHANGE,
                                RabbitMqConfig.TITLE_GENERATOR_ROUTING_KEY,
                                titleMessage
                        );
                        break;
                    }
                }
            }

            // 更新会话记录的更新时间
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }

        String key = RedisCacheConstants.AI_CREATE + memoryId;

        // 1. 写入Redis（实时缓存）
        String json = ChatMessageSerializer.messagesToJson(messages);
        stringRedisTemplate.opsForValue().set(key, json, Duration.ofDays(1));

        // 2. 写入MySQL（持久化，避免阻塞Redis操作）
        // 只保存最新的消息（避免重复写入）
        ChatMessage lastMessage = messages.get(messages.size() - 1);

        String content = getMessageContent(lastMessage);
        if (!content.isEmpty()) {
            AiChatMessage message = new AiChatMessage();
            message.setSessionId(sessionId);
            message.setContent(getMessageContent(lastMessage));
            message.setMessageType(lastMessage instanceof UserMessage ? 1 : 2);
            messageMapper.insert(message);
        }
    }
    /**
     * 适配 langchain4j 1.2.0-beta8：获取不同类型 ChatMessage 的文本内容
     * @param chatMessage 聊天消息
     * @return 文本内容（空字符串兜底）
     */
    private String getMessageContent(ChatMessage chatMessage) {
        try {
            if (chatMessage instanceof UserMessage userMessage) {
                return userMessage.singleText();
            } else if (chatMessage instanceof AiMessage) {
                return ((AiMessage) chatMessage).text();
            } else {
                // 其他消息类型兜底
                return "";
            }
        } catch (Exception e) {
            // 异常兜底，避免插入失败
            return "";
        }

    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = RedisCacheConstants.AI_CREATE + memoryId;
        stringRedisTemplate.delete(key);
        // 可选：逻辑删除MySQL记录或保留（根据业务需求）
    }
}
