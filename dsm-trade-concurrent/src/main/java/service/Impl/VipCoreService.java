package service.Impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.util.SecurityUtil;
import constants.VipRedisConstant;
import entity.Vip;
import entity.vo.VipStatusVO;
import lombok.RequiredArgsConstructor;


import mapper.VipMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import service.IVipCoreService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static constants.VipRedisConstant.*;


/**
 *  Vip核心服务
 *  权益管理
 *  状态管理
 *  等级控制
 */
@RequiredArgsConstructor
@Service
public class VipCoreService implements IVipCoreService {
    private final VipMapper vipMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 检查并更新用户VIP状态
     */
    @Override
    public VipStatusVO checkAndUpdateVipStatus() {
        // 1. 查询用户的VIP记录
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("尚未登陆");
        }
        String vipInfoJson = stringRedisTemplate.opsForValue().get(USER_VIP_INFO_PREFIX + userId);

        if (vipInfoJson == null) {
            //  缓存未命中，redis没有数据，从数据库查询
            // 从数据库查询用户最新的VIP记录（优先查生效中的，无则查历史记录）
            Vip vip = vipMapper.selectOne(new LambdaQueryWrapper<Vip>()
                    .eq(Vip::getUserId, userId)
                    .orderByDesc(Vip::getEndTime) // 取最新的一条记录, 避免仅查询id查到过期vip
                    .last("LIMIT 1"));

            if (vip == null) {
                // 2.1 数据库无记录（从未开通VIP）：缓存空值
                //  缓存穿透 -> 解决方法：缓存空值
                stringRedisTemplate.opsForValue().set(USER_VIP_INFO_PREFIX + userId, "", CACHE_NULL_EXPIRE_TIME, TimeUnit.SECONDS);
                VipStatusVO defaultStatus = new VipStatusVO();
                defaultStatus.setIsVip(0);
                defaultStatus.setVipExpireTime(LocalDateTime.MIN);
                defaultStatus.setRemainingDays(0);
                defaultStatus.setVipLevel(0);
                defaultStatus.setVipPermissions(null);
                defaultStatus.setFreeVipUsed(0);
                defaultStatus.setLastPackageName(null);
                return defaultStatus;
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
                vipStatusVO.setRemainingDays(0);
            } else {
                // 未过期：缓存过期时间 = VIP剩余有效期（秒），最多不超过24小时（避免缓存时间过长）
                long remainingSeconds = Duration.between(now, vip.getEndTime()).getSeconds();
                cacheExpireSeconds = Math.min(remainingSeconds, VIP_CACHE_USER_INFO_24H); // 24小时上限
                vipStatusVO.setIsVip(1);
                vipStatusVO.setRemainingDays(Math.toIntExact(Duration.between(now, vip.getEndTime()).toDays()));
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
            throw new RuntimeException("VIP信息解析失败", e);
        }

        // 2. 检查VIP是否已过期
        if (vipStatusVO.getVipExpireTime().isBefore(LocalDateTime.now())) {
            // 3. 更新过期VIP的状态为EXPIRED
            vipStatusVO.setIsVip(0);
            throw new RuntimeException("VIP已过期");
        }

        // 4. 发送状态变更通知
        // 5. 记录状态变更日志
        return vipStatusVO;
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

    /**
     * 处理VIP自动续费
     */
    @Override
    public void processAutoRenewal() {
        // 1. 查询即将到期且开启自动续费的VIP用户
        // 2. 调用支付服务执行自动扣款
        // 3. 根据扣款结果更新VIP有效期
        // 4. 发送续费成功/失败通知
        // 5. 记录续费操作日志
    }

//    /**
//     * 升级用户VIP等级
//     */
//    public void upgradeVipLevel(Long userId, Integer targetLevel) {
//        // 1. 验证目标等级的有效性
//        // 2. 检查用户当前VIP状态
//        // 3. 计算升级费用和新的有效期
//        // 4. 更新用户VIP等级和权益
//        // 5. 发送升级成功通知
//    }
}
