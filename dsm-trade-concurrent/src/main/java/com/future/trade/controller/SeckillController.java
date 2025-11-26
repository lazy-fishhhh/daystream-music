package com.future.trade.controller;


import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.service.Impl.SeckillActivityService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("秒杀业务")
@RequiredArgsConstructor
@RestController
@RequestMapping("/seckill")
public class SeckillController {
    private final SeckillActivityService seckillActivityService;

    /**
     * 新增秒杀商品（需管理员权限）
     */
    @PostMapping("/addActivity")
    @ApiOperation("新增秒杀商品")
//    @PreAuthorize("hasRole('ADMIN')") //
    public Result<String> addSeckillActivity(@Valid @RequestBody SeckillActivityDTO req) {
            return seckillActivityService.addSeckillActivity(req);
    }

}
