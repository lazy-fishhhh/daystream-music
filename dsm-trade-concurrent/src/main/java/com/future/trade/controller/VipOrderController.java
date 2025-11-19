package com.future.trade.controller;


import com.future.trade.service.IVipOrderService;
import entity.pojo.util.Result;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ApiOperation("VIP订单管理")
@RestController
@RequestMapping("/vip/order")
@RequiredArgsConstructor
public class VipOrderController {

    private final IVipOrderService vipOrderService;

    @ApiOperation("购买VIP")
    @GetMapping("/buyVip")
    public Result<String> buyVip(@RequestParam String packageName) {
        return vipOrderService.buyVip(packageName);
    }



}
