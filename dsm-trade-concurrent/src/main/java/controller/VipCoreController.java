package controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;


@ApiOperation("VIP核心服务")
@RestController
@RequestMapping("/vip/core")
public class VipCoreController {

    /**
     * 检查用户VIP状态
     */
//    public boolean isUserVip(Long userId) {
//        // 先查缓存
//        String cacheKey = "user_vip:" + userId;
//        Boolean isVip = (Boolean) redisTemplate.opsForValue().get(cacheKey);
//        if (isVip != null) {
//            return isVip;
//        }
//
//        // 查数据库
//        UserVip userVip = userVipMapper.selectByUserId(userId);
//        boolean vip = userVip != null && userVip.getStatus() == 1 && userVip.getEndTime().after(new Date());
//
//        // 写入缓存
//        redisTemplate.opsForValue().set(cacheKey, vip, Duration.ofMinutes(10));
//
//        return vip;
//    }
}
