package com.future.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.trade.entity.SeckillActivity;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    // 扣减数据库库存（乐观锁防超卖）
    @Update("UPDATE seckill_activity SET remaining_stock = remaining_stock - 1 WHERE id = #{activityId} AND remaining_stock > 0")
    int decreaseStock(Long activityId);

    // 回滚数据库库存
    @Update("UPDATE seckill_activity SET remaining_stock = remaining_stock + 1 WHERE id = #{activityId}")
    int increaseStock(Long activityId);
}