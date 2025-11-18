package com.future.trade.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import entity.pojo.util.Result;
import com.future.trade.entity.vo.VipStatusVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.future.trade.service.Impl.VipCoreService;


@Api("VIP核心服务")
@RestController
@RequestMapping("/vip/core")
@RequiredArgsConstructor
public class VipCoreController {


    private final VipCoreService vipCoreService;

    /**
     * 检查用户VIP状态
     */
    @ApiOperation("检查用户VIP状态")
    @GetMapping("/vipStatus")
    public Result<VipStatusVO> isUserVip() throws JsonProcessingException {
            return Result.ok(vipCoreService.checkAndUpdateVipStatus());

    }


    /**
     * 购买 VIP
     */
    @ApiOperation("购买 VIP")
    @GetMapping("/buyVip")
    public Result<String> buyVip(@RequestParam String packageName)  {
        return vipCoreService.buyVip(packageName);
    }
}
