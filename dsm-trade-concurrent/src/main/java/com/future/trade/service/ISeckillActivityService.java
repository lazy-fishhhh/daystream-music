package com.future.trade.service;

import com.future.trade.entity.SeckillActivity;
import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.entity.vo.SeckillActivityVO;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 秒杀活动服务接口
 */
public interface ISeckillActivityService {

    /**
     * 新增秒杀活动
     * @param dto 秒杀活动DTO
     * @return 结果
     */
    Result<String> addSeckillActivity(@Valid SeckillActivityDTO dto);

    /**
     * 购买秒杀商品
     * @param seckillActivityId 秒杀活动ID
     * @return 结果
     */
    Result<String> purchaseSeckillProduct(Long seckillActivityId);

    /**
     * 获取所有秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 秒杀商品分页列表
     */
    Result<PageResp<SeckillActivityVO>> getAllSeckillProducts(PageReq pageReq);

    /**
     * 获取即将开启的秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 即将开启的秒杀商品分页列表
     */
    Result<PageResp<SeckillActivityVO>> getUpcomingSeckillProducts(PageReq pageReq);

    /**
     * 获取正在进行中的秒杀商品（分页）
     * @param pageReq 分页请求参数
     * @return 正在进行中的秒杀商品分页列表
     */
    Result<PageResp<SeckillActivityVO>> getOngoingSeckillProducts(PageReq pageReq);
}
