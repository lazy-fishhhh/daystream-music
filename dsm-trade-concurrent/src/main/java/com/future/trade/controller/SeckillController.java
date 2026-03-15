package com.future.trade.controller;


import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.service.ISeckillActivityService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import util.JwtUtil;

import static util.JwtUtil.createJWT;

@Api("秒杀业务")
@RequiredArgsConstructor
@RestController
@RequestMapping("/seckill")
@Validated
public class SeckillController {
    private final ISeckillActivityService seckillActivityService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成1000token用于测试并发
     */
    @GetMapping("/generateToken")
    @ApiOperation("生成1000token用于测试并发")
    public void generateToken() {
        // 生成1000个token 用于测试并发
        for (int i = 0; i < 1000; i++) {
            String token = createJWT(String.valueOf(i), 1000 * 60 * 5L);
            stringRedisTemplate.opsForValue().set("testConcurrentToken:" + i, token, 1000 * 60 * 5L);
            System.out.println(token);
        }
    }

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


    @GetMapping("/delete")
    @ApiOperation("删除任意的秒杀商品")
    public Result<Boolean> deleteSeckillActivity(@RequestParam Long seckillActivityId) {
        return seckillActivityService.delete(seckillActivityId);
    }

}