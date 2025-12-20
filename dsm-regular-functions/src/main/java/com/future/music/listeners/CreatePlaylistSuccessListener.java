package com.future.music.listeners;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.future.login.exception.BusinessException;
import com.future.music.entity.message.CreatePlaylistMessage;
import com.future.music.mapper.UserTaskMapper;
import entity.pojo.info.UserTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

import static constants.RabbitConstants.*;

@RequiredArgsConstructor
@Slf4j
@Component

public class CreatePlaylistSuccessListener {

    private final UserTaskMapper userTaskMapper;

    @RabbitListener(
            queues = CREATE_PLAYLIST_SUCCESS_QUEUE
    )
    @Transactional(rollbackFor = Exception.class)
    public void createPlaylistSuccessListener(CreatePlaylistMessage message) {
        // 1. 前置参数校验：避免空指针和无效参数
        try {
            Assert.notNull(message, "创建歌单消息体不能为空");
            Assert.notNull(message.getUserId(), "用户ID不能为空");
            Assert.notNull(message.getTaskId(), "任务ID不能为空");

            log.info("CreatePlaylistSuccessListener收到第一次创建歌单任务成功消息：{},userId:{}", message, message.getUserId());

            // 2. 增强幂等性查询：仅查询未完成的任务（避免并发更新）
            LambdaQueryWrapper<UserTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserTask::getUserId, message.getUserId())
                    .eq(UserTask::getTaskId, message.getTaskId())
                    .eq(UserTask::getTaskStatus, 0);
            UserTask userTask = userTaskMapper.selectOne(queryWrapper);

            // 3. 业务逻辑处理
            if (userTask == null) {
                log.warn("用户任务不存在或已完成，userId:{}, taskId:{}", message.getUserId(), message.getTaskId());
                throw new BusinessException("用户任务不存在或已完成");
            }

            // 4. 更新任务状态
            userTask.setTaskStatus(1);
            userTask.setCompleteTime(LocalDateTime.now());
            int updateCount = userTaskMapper.updateById(userTask);

            if (updateCount <= 0) {
                log.error("更新用户任务状态失败，userId:{}, taskId:{}", message.getUserId(), message.getTaskId());
                throw new BusinessException("更新创建歌单任务状态失败");
            }

            log.info("用户任务更新成功，userId:{}, taskId:{}", message.getUserId(), message.getTaskId());

        } catch (IllegalArgumentException e) {
            // 参数校验异常：拒绝重发，进入死信队列
            log.error("参数校验失败，message:{}", message, e);
            throw new AmqpRejectAndDontRequeueException("参数非法，拒绝重发", e);
        } catch (BusinessException e) {
            // 业务异常：拒绝重发
            log.error("处理创建歌单任务业务异常，userId:{}, taskId:{}",
                    message != null ? message.getUserId() : "null",
                    message != null ? message.getTaskId() : "null", e);
            throw new AmqpRejectAndDontRequeueException("业务异常，拒绝重发", e);
        } catch (Exception e) {
            // 未知异常：触发重试（如数据库临时故障）
            log.error("处理创建歌单任务未知异常，userId:{}, taskId:{}",
                    message != null ? message.getUserId() : "null",
                    message != null ? message.getTaskId() : "null", e);
            throw new RuntimeException("处理消息异常，触发重试", e);
        }
    }
}