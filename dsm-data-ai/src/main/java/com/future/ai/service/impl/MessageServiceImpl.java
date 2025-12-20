package com.future.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.ai.domain.entity.AiChatMessage;
import com.future.ai.domain.entity.AiChatSession;
import com.future.ai.mapper.AiChatMessageMapper;
import com.future.ai.mapper.AiChatSessionMapper;
import com.future.ai.service.MessageService;
import com.future.login.util.SecurityUtil;
import constants.RedisCacheConstants;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    private AiChatSessionMapper aiChatSessionMapper;

    @Resource
    private AiChatMessageMapper aiChatMessageMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据会话id查询消息记录
     * @param memoryId 消息ID
     * @return 消息内容
     */
    @Override
    public Result<List<AiChatMessage>> getMessage(String memoryId, Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        Long mId = Long.parseLong(memoryId);
        QueryWrapper<AiChatSession> sessionQueryWrapper = new QueryWrapper<AiChatSession>()
                .eq("id",mId)
                .eq("user_id",userId);
        if(aiChatSessionMapper.selectOne(sessionQueryWrapper) == null){
            return Result.fail("会话不存在/用户权限不足");
        }
        QueryWrapper<AiChatMessage> messageQueryWrapper = new QueryWrapper<AiChatMessage>()
                .eq("session_id",mId)
                .orderByAsc("send_time");
        Page<AiChatMessage> page = new Page<>(pageNum,pageSize);
        aiChatMessageMapper.selectPage(page,messageQueryWrapper);
        if (page.getRecords().isEmpty()) {
            return Result.ok(List.of());
        }

        return Result.ok(page.getRecords());
    }

    /**
     * 删除会话
     * @param memoryId 会话ID
     */
    @Override
    public Result<Void> deleteMessages(String memoryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        Long mId = Long.parseLong(memoryId);
        QueryWrapper<AiChatSession> sessionQueryWrapper = new QueryWrapper<AiChatSession>()
                .eq("id",mId)
                .eq("user_id",userId);
        if(aiChatSessionMapper.selectOne(sessionQueryWrapper) == null){
            return Result.fail("会话不存在/用户权限不足");
        }
        QueryWrapper<AiChatMessage> messageQueryWrapper = new QueryWrapper<AiChatMessage>()
                .eq("session_id",mId);
        aiChatMessageMapper.delete(messageQueryWrapper);
        aiChatSessionMapper.deleteById(mId);
        String key = RedisCacheConstants.AI_CREATE + memoryId;
        stringRedisTemplate.delete(key);

        return Result.ok();
    }
}
