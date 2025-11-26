package com.future.trade.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import com.future.trade.service.IBasicOrderService;
import constants.UserConstants;
import entity.pojo.info.User;
import entity.pojo.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;



@Validated
@RequiredArgsConstructor
@Service
@Slf4j
public class BasicOrderService implements IBasicOrderService {

    private final UserMapper userMapper;

    /**
     * 打赏功能
     * @param receiverId 接收人ID
     * @param amount 打赏金额
     * @return 打赏结果
     *
     * 核心是，采用乐观锁机制，确保在并发环境下，用户余额不会被错误地修改。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<String> rewardBasicOrder(Long receiverId, Integer amount) {
        // 1. 获取当前登录用户
        Long giverId = SecurityUtil.getCurrentUserId();
        if (giverId == null) {
            return Result.fail("用户未登录");
        }
        if (giverId.equals(receiverId)) {
            return Result.fail("不能给自己打赏");
        }

        try {
            // ====================== 扣减打赏人余额 ======================

            // 1. 查询打赏人当前信息
            User giver = userMapper.selectById(giverId);
            if (giver == null) {
                log.warn("打赏人不存在：giverId={}", giverId);
                return Result.fail("打赏人不存在");
            }
            if (giver.getWalletBalance() < amount) {
                log.warn("打赏人余额不足：giverId={}, balance={}, amount={}", giverId, giver.getWalletBalance(), amount);
                return Result.fail("您的余额不足~");
            }
            Integer originalGiverBalance = giver.getWalletBalance(); // 记录查询到的原始余额

            // 2. 执行扣减更新，WHERE条件包含原始余额
            LambdaUpdateWrapper<User> giverUpdateWrapper = new LambdaUpdateWrapper<>();
            giverUpdateWrapper.eq(User::getId, giverId)
                    .eq(User::getWalletBalance, originalGiverBalance) // 乐观锁机制，确保余额未变
                    .setSql("wallet_balance = wallet_balance - " + amount);
            int giverUpdateCount = userMapper.update(null, giverUpdateWrapper);

            // 3. 检查更新是否成功
            if (giverUpdateCount == 0) {
                log.warn("打赏人余额更新失败，可能并发修改：giverId={}, originalBalance={}", giverId, originalGiverBalance);
                return Result.fail("操作过于频繁，请稍后重试");
            }

            // ====================== 增加接收人余额 ======================

            // 1. 查询接收人当前信息
            User receiver = userMapper.selectById(receiverId);
            if (receiver == null) {
                log.warn("接收人不存在：receiverId={}", receiverId);
                // 由于事务特性，上面扣减的钱会自动回滚
                throw new BusinessException(UserConstants.RECEIVER_NOT_FOUND, "接收人不存在");

            }
            Integer originalReceiverBalance = receiver.getWalletBalance();

            // 2. 执行增加更新
            LambdaUpdateWrapper<User> receiverUpdateWrapper = new LambdaUpdateWrapper<>();
            receiverUpdateWrapper.eq(User::getId, receiverId)
                    .eq(User::getWalletBalance, originalReceiverBalance)
                    .setSql("wallet_balance = wallet_balance + " + amount);
            int receiverUpdateCount = userMapper.update(null, receiverUpdateWrapper);

            // 3. 检查更新是否成功
            if (receiverUpdateCount == 0) {
                log.warn("接收人余额更新失败，可能并发修改：receiverId={}, originalBalance={}", receiverId, originalReceiverBalance);
                // 抛出异常触发事务回滚
                throw new RuntimeException("操作过于频繁，打赏失败");
            }

        } catch (RuntimeException e) {
            log.error("打赏业务异常：giverId={}, receiverId={}, amount={}", giverId, receiverId, amount, e);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("打赏系统异常：giverId={}, receiverId={}, amount={}", giverId, receiverId, amount, e);
            return Result.fail("系统异常，打赏失败，请稍后重试");
        }

        return Result.ok("打赏成功~");
    }
}