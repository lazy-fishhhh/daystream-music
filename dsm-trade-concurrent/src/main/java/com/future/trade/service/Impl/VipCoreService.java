package com.future.trade.service.Impl;


import cn.hutool.core.bean.BeanUtil;
import code.BusinessErrorCode;
import code.UserErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.exception.BusinessException;
import com.future.login.exception.VipStatusException;
import com.future.login.util.SecurityUtil;
import com.future.trade.constants.VipRedisConstant;
import com.future.trade.entity.Vip;
import com.future.trade.entity.vo.VipStatusVO;

import entity.pojo.util.Result;
import lombok.RequiredArgsConstructor;


import com.future.trade.mapper.VipMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.future.trade.service.IVipCoreService;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.future.trade.constants.VipRedisConstant.*;


/**
 *  Vip核心服务
 *  权益管理
 *  状态管理
 *  等级控制
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class VipCoreService implements IVipCoreService {
    private final VipMapper vipMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;


    /**
     * 创建默认的VIP状态（未开通VIP时使用）
     */
    private VipStatusVO defaultVipStatus() {
        VipStatusVO defaultStatus = new VipStatusVO();
        defaultStatus.setIsVip(0); // 非VIP
        defaultStatus.setVipExpireTime(LocalDateTime.MIN); // 初始化为最小时间，避免null
        defaultStatus.setRemainingDays(0); // 剩余天数为0
        defaultStatus.setVipLevel(0); // 等级为0
        defaultStatus.setCurrentPackageName(null); // 无套餐名称
        return defaultStatus;
    }


    /**
     * 检查并更新用户VIP状态
     */
    @Override
    public VipStatusVO checkAndUpdateVipStatus() throws JsonProcessingException {
        // 1. 查询用户的VIP记录
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("尚未登陆");
        }
        String vipInfoJson = stringRedisTemplate.opsForValue().get(USER_VIP_INFO_PREFIX + userId);

        if (vipInfoJson == null) {
            // 缓存未命中，redis没有数据，从数据库查询
            // 从数据库查询用户最新的VIP记录（优先查生效中的，无则查历史记录）
            Vip vip = vipMapper.selectOne(new LambdaQueryWrapper<Vip>()
                    .eq(Vip::getUserId, userId)
                    .orderByDesc(Vip::getEndTime) // 取最新的一条记录, 避免仅查询id查到过期vip
                    .last("LIMIT 1"));

            if (vip == null) {
                // 2.1 数据库无记录（从未开通VIP）：缓存空值
                // 缓存穿透 -> 解决方法：缓存空值
                // 缓存序列化后的空对象
                stringRedisTemplate.opsForValue().set(USER_VIP_INFO_PREFIX + userId, objectMapper.writeValueAsString(defaultVipStatus()), CACHE_NULL_EXPIRE_TIME, TimeUnit.SECONDS);
                return defaultVipStatus();
            }

            // 2.2 数据库有记录：检查是否过期，更新状态后缓存
            VipStatusVO vipStatusVO = BeanUtil.copyProperties(vip, VipStatusVO.class);
            boolean isExpired = vip.getEndTime().isBefore(LocalDateTime.now());
            LocalDateTime now = LocalDateTime.now();

            // 计算缓存过期时间
            long cacheExpireSeconds;
            if (isExpired) {
                // 已过期：缓存短期保留（5分钟），避免频繁查询数据库
                cacheExpireSeconds = CACHE_NULL_EXPIRE_TIME;
                vip.setStatus(Vip.VipStatus.EXPIRED);
                vipMapper.updateById(vip);
                vipStatusVO.setIsVip(0);
                vipStatusVO.setVipExpireTime(LocalDateTime.MIN);
                vipStatusVO.setVipLevel(0);
                vipStatusVO.setRemainingDays(0);
            } else {
                // 未过期：缓存过期时间 = VIP剩余有效期（秒），最多不超过24小时（避免缓存时间过长）
                long remainingSeconds = Duration.between(now, vip.getEndTime()).getSeconds();
                cacheExpireSeconds = Math.min(remainingSeconds, VIP_CACHE_USER_INFO_24H); // 24小时上限
                vipStatusVO.setIsVip(1);
                vipStatusVO.setVipExpireTime(vip.getEndTime());
                vipStatusVO.setVipLevel(vip.getLevel());

                // 计算剩余天数（不满1天按1天算，刚好1天及以上按实际天数算）
                long fullDays = Duration.between(now, vip.getEndTime()).toDays(); // 完整天数（向下取整）
                long remainingDays;
                if (fullDays > 0) {
                    // 超过1天：直接用完整天数（例如1天1小时 → 1天）
                    remainingDays = fullDays;
                } else {
                    // 不足1天：按1天算（但需确保未过期）
                    remainingDays = remainingSeconds > 0 ? 1 : 0;
                }
                vipStatusVO.setRemainingDays(Math.toIntExact(remainingDays));
            }

            // 缓存处理后的状态（使用计算出的过期时间）
            try {
                stringRedisTemplate.opsForValue().set(
                        USER_VIP_INFO_PREFIX + userId,
                        objectMapper.writeValueAsString(vipStatusVO),
                        cacheExpireSeconds, // 动态设置过期时间
                        TimeUnit.SECONDS
                );

            } catch (Exception e) {
                throw new RuntimeException("VIP信息序列化失败", e);
            }
            return vipStatusVO;
        }


        // 缓存命中
        // 反序列化
        VipStatusVO vipStatusVO;
        try {
            vipStatusVO = objectMapper.readValue(vipInfoJson, VipStatusVO.class);
        } catch (Exception e) {
            // 反序列化失败时，删除错误缓存，下次查询走数据库
            stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + userId);
            throw new JsonParseException("VIP信息解析失败");
        }


        // 2. 检查VIP是否已过期
        if (vipStatusVO.getVipExpireTime().isBefore(LocalDateTime.now())) {
            // 3. 更新过期VIP的状态为EXPIRED
            vipStatusVO.setIsVip(0);
            throw VipStatusException.expired(userId, vipStatusVO.getVipExpireTime());
        }

        // TODO 4. 发送状态变更通知
        // TODO 5. 记录状态变更日志
        return vipStatusVO;
    }


    /**
     * 开启/关闭自动续费
     */
    @Transactional
    @Override
    public Result<String> toggleAutoRenew(Integer autoRenew) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(UserErrorCode.LOGIN_STATE_INVALID.getCode(), "用户未登录");
        }
        if (autoRenew == null || (autoRenew != 0 && autoRenew != 1)) {
            throw new BusinessException(BusinessErrorCode.PARAM_ERROR.getCode(), "无效的自动续费状态（仅支持0/1）");
        }

        // 查询用户当前有效的VIP记录
        Vip vip = vipMapper.selectOne(new LambdaQueryWrapper<Vip>()
                .eq(Vip::getUserId, userId)
                .eq(Vip::getStatus, Vip.VipStatus.ACTIVE)
                .orderByDesc(Vip::getEndTime)
                .last("LIMIT 1"));
        if (vip == null) {
            throw VipStatusException.recordNotFound(userId);
        }

        // 更新自动续费状态
        vip.setAutoRenew(autoRenew == 1);
        vip.setUpdatedTime(LocalDateTime.now());
        vipMapper.updateById(vip);

        // 清除VIP缓存（刷新状态）
        stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + userId);

        String msg = autoRenew == 1 ? "自动续费开启成功" : "自动续费关闭成功";
        log.info("用户[{}]{}，VIP记录ID：{}", userId, msg, vip.getId());
        return Result.ok(msg);
    }


    /**
     * 验证用户是否享有特定VIP权益
     */
//    public boolean validateUserBenefit(Long userId, VipBenefit benefit) {
//        // 1. 检查用户VIP状态是否有效
//        // 2. 验证VIP等级是否满足权益要求
//        // 3. 检查权益是否在有效期内
//        // 4. 验证权益使用次数限制
//        // 5. 返回权益验证结果
//    }





}
