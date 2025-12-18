package com.future.trade.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.entity.vo.SeckillActivityVO;
import com.future.trade.entity.vo.VipStatusVO;
import com.future.trade.service.ISeckillActivityService;
import com.future.trade.service.Impl.VipCoreService;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Api("VIP核心服务")
@RestController
@RequestMapping("/vip/core")
@RequiredArgsConstructor
public class VipCoreController {


    private final VipCoreService vipCoreService;
    private final ISeckillActivityService seckillActivityService;

    /**
     * 检查用户VIP状态
     */
    @ApiOperation("检查用户VIP状态")
    @GetMapping("/vipStatus")
    public Result<VipStatusVO> isUserVip() throws JsonProcessingException {
            return Result.ok(vipCoreService.checkAndUpdateVipStatus());
    }

    /**
     * 开启/关闭自动续费
     */
    @ApiOperation("开启/关闭自动续费")
    @GetMapping("/autoRenew")
    public Result<String> enableAutoRenew(@RequestParam Integer autoRenew) {
        return vipCoreService.toggleAutoRenew(autoRenew);
    }

    /**
     * 获取所有秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 所有秒杀商品分页列表
     */
    @ApiOperation("获取所有秒杀商品")
    @GetMapping("/seckill/all")
    public Result<PageResp<SeckillActivityVO>> getAllSeckillProducts(@Validated @ModelAttribute PageReq pageReq) {
        return seckillActivityService.getAllSeckillProducts(pageReq);
    }

    /**
     * 获取即将开启的秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 即将开启的秒杀商品分页列表
     */
    @ApiOperation("获取即将开启的秒杀商品")
    @GetMapping("/seckill/upcoming")
    public Result<PageResp<SeckillActivityVO>> getUpcomingSeckillProducts(@Validated @ModelAttribute PageReq pageReq) {
        return seckillActivityService.getUpcomingSeckillProducts(pageReq);
    }

    /**
     * 获取正在进行中的秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 正在进行中的秒杀商品分页列表
     */
    @ApiOperation("获取正在进行中的秒杀商品")
    @GetMapping("/seckill/ongoing")
    public Result<PageResp<SeckillActivityVO>> getOngoingSeckillProducts(@Validated @ModelAttribute PageReq pageReq) {
        return seckillActivityService.getOngoingSeckillProducts(pageReq);
    }

    /**
     * 修改未开始的秒杀商品
     * @param activityId 秒杀活动ID
     * @param dto 秒杀活动DTO
     * @return 结果
     */
    @ApiOperation("修改未开始的秒杀商品")
    @PutMapping("/seckill/{activityId}")
    public Result<String> updateSeckillActivity(@PathVariable Long activityId, @Validated @RequestBody SeckillActivityDTO dto) {
        return seckillActivityService.updateSeckillActivity(activityId, dto);
    }

    /**
     * 删除秒杀商品
     * @param activityId 秒杀活动ID
     * @return 结果
     */
    @ApiOperation("删除秒杀商品")
    @DeleteMapping("/seckill/{activityId}")
    public Result<String> deleteSeckillActivity(@PathVariable Long activityId) {
        return seckillActivityService.deleteSeckillActivity(activityId);
    }
    
}
