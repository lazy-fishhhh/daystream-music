package com.future.trade.controller;


import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.service.ISeckillActivityService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api("秒杀业务")
@RequiredArgsConstructor
@RestController
@RequestMapping("/seckill")
@Validated
public class SeckillController {
    private final ISeckillActivityService seckillActivityService;

    /**
     * 新增秒杀商品（需管理员权限）
     */
    @PostMapping("/addActivity")
    @ApiOperation("新增秒杀商品（添加的秒杀或商品必须是已存在的商品）")
//    @PreAuthorize("hasRole('ADMIN')") //
    public Result<String> addSeckillActivity(@Valid @RequestBody SeckillActivityDTO req) {
            return seckillActivityService.addSeckillActivity(req);
    }

    /**
     * 秒杀商品
     */
     @GetMapping("/purchase")
     @ApiOperation("秒杀商品（一人一单）")
    public Result<String> purchaseSeckillProduct(@RequestParam Long seckillActivityId) {
        return seckillActivityService.purchaseSeckillProduct(seckillActivityId);
    }
}
