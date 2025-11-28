package com.future.trade.service;

import com.future.trade.entity.SeckillActivity;
import com.future.trade.entity.SeckillOrder;
import com.future.trade.entity.dto.SeckillActivityDTO;
import com.future.trade.entity.vo.SeckillActivityVO;
import entity.pojo.util.Result;
import jakarta.validation.Valid;



public interface ISeckillActivityService {

    /**
     * 添加秒杀活动
     * @param req 秒杀活动DTO
     * @return 秒杀活动VO
     */
    public Result<String> addSeckillActivity(@Valid SeckillActivityDTO req);

    Result<String> purchaseSeckillProduct(Long seckillActivityId);

//    SeckillOrder createSeckillOrder(Long seckillActivityId, Long userId, SeckillActivity activity);
}
