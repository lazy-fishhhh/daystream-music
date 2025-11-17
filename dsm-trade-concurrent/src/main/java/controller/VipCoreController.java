package controller;

import entity.pojo.util.Result;
import entity.vo.VipStatusVO;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.Impl.VipCoreService;


@ApiOperation("VIP核心服务")
@RestController
@RequestMapping("/vip/core")
@RequiredArgsConstructor
public class VipCoreController {

    private final StringRedisTemplate stringRedisTemplate;
    private final VipCoreService vipCoreService;

    /**
     * 检查用户VIP状态
     */
    public Result<VipStatusVO> isUserVip() {
        return Result.ok(vipCoreService.checkAndUpdateVipStatus());
    }
}
