package com.future.trade.controller;


import com.future.trade.entity.vo.OrderDetailVO;
import com.future.trade.service.IVipOrderService;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    
    @ApiOperation("查询历史订单")
    @GetMapping("/getUserOrderHistory")
    // @ModelAttribute 用于将请求参数绑定到复杂对象（如自定义 POJO）
    // Spring 会根据请求参数的键名匹配对象的字段名，自动完成赋值。
    // 这里其实不用显示地使用@ModelAttribute
    // 若省略 @ModelAttribute 注解，
    // Spring 会默认对复杂对象（非简单类型，如 User、PageReq）使用参数绑定
    // 效果与添加 @ModelAttribute 一致
    public Result<PageResp<OrderDetailVO>> getUserOrderHistory(@Validated @ModelAttribute PageReq pageReq) {
        return vipOrderService.getUserOrderHistory(pageReq);
    }



}
