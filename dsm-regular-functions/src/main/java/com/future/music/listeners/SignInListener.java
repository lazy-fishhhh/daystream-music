package com.future.music.listeners;


import cn.hutool.crypto.asymmetric.Sign;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.future.login.mapper.UserMapper;
import com.future.music.domain.message.SignInMessage;
import entity.pojo.info.User;
import kotlin.jvm.internal.Lambda;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static constants.RabbitConstants.*;


@Slf4j
@RequiredArgsConstructor
@Component
public class SignInListener {

    private final UserMapper userMapper;
        @RabbitListener(bindings = @QueueBinding(
                value = @Queue(value = SIGN_IN_SUCCESS_QUEUE, durable = "true", declare = "true"),
                exchange = @Exchange(value = SIGN_IN_SUCCESS_TOPIC_EXCHANGE,
                        type = ExchangeTypes.TOPIC, durable = "true"),
                key = ROUTING_KEY_SIGN_IN_SUCCESS
        ))
        public void signInMessageListener(SignInMessage message) {
            log.info("SignInMessageListener收到签到成功消息：{}", message);
            // 原子性更新
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, message.getUserId())
                    // 1. 更新上次签到时间
                    .set(User::getLastCheckinTime, message.getSignInTime())
                    // 2. 给予签到奖励
                    .setSql("wallet_balance = wallet_balance + " + message.getReward());

            // 执行更新（原子操作，避免先查再改的并发问题）
            int updateRows = userMapper.update(null, updateWrapper);

            if (updateRows == 0) {
                log.error("用户{}签到更新失败（用户不存在或无更新）", message.getUserId());
                // 可以抛异常触发MQ重试
            } else {
                LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(User::getId, message.getUserId());
                User user = userMapper.selectOne(queryWrapper);
                log.info("签到成功，用户{} 签到时间：{}", user.getUsername(), message.getSignInTime());
            }
        }
}
