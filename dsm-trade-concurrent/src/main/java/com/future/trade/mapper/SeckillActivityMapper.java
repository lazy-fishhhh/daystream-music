package com.future.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.trade.entity.SeckillActivity;
import com.future.trade.entity.vo.SeckillActivityVO;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 秒杀活动 Mapper 层
 * 负责秒杀活动的库存操作、分页查询等数据库交互
 */
@Repository
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    Logger log = LoggerFactory.getLogger(SeckillActivityMapper.class);

    /**
     * 扣减数据库库存（乐观锁防超卖：版本号 + 库存校验）
     *
     * @param activityId 秒杀活动ID
     * @return 影响行数（1=成功，0=失败）
     */
    @Update("UPDATE seckill_activity " +
            "SET remaining_stock = remaining_stock - 1,  update_time = NOW() " +
            "WHERE id = #{activityId} AND remaining_stock > 0")
    int decreaseStock(@Param("activityId") Long activityId);

    /**
     * 回滚数据库库存（补偿操作）
     *
     * @param activityId 秒杀活动ID
     * @return 影响行数
     */
    @Update("UPDATE seckill_activity " +
            "SET remaining_stock = remaining_stock + 1, update_time = NOW() " +
            "WHERE id = #{activityId} AND remaining_stock < total_stock") // 防止库存超总数量
    int increaseStock(@Param("activityId") Long activityId);


    /**
     * 分页查询所有秒杀活动（排除已结束状态）
     * 核心逻辑：original_price = 0 或 ≤ seckill_price 时，设为 seckill_price * 1.25
     *
     * @param page           分页参数
     * @param productTypeVip 会员套餐产品类型编码
     * @return 秒杀活动VO分页结果
     */
    @Select("SELECT sa.id as activity_id," +
            "sa.activity_name," +
            "sa.total_stock," +
            "sa.remaining_stock," +
            "sa.seckill_price," +
            "sa.start_time," +
            "sa.end_time," +
            "sa.status," +
            "sa.product_type," +
            "sa.product_id" +
            ", " +
            "CASE " +
            "   WHEN (vp.original_price IS NULL OR vp.original_price = 0 OR vp.original_price <= sa.seckill_price) " +
            "   THEN sa.seckill_price * 1.25 " +
            "   ELSE vp.original_price " +
            "END as original_price " +
            "FROM seckill_activity sa " +
            "LEFT JOIN vip_package vp ON sa.product_type = #{productTypeVip} AND sa.product_id = vp.id " +
            "WHERE sa.status != 2 " +
            "ORDER BY sa.start_time ASC")
    IPage<SeckillActivityVO> selectAllSeckillActivities(Page<SeckillActivity> page,
                                                        @Param("productTypeVip") Integer productTypeVip);

    /**
     * 分页查询即将开始的秒杀活动（未开始 + 已启用）
     * 核心逻辑：original_price = 0 或 ≤ seckill_price 时，设为 seckill_price * 1.25
     *
     * @param page           分页参数
     * @param currentTime    当前时间
     * @param productTypeVip 会员套餐产品类型编码
     * @return 秒杀活动VO分页结果
     */
    @Select("SELECT sa.id as activity_id," +
            "sa.activity_name," +
            "sa.total_stock," +
            "sa.remaining_stock," +
            "sa.seckill_price," +
            "sa.start_time," +
            "sa.end_time," +
            "sa.status," +
            "sa.product_type," +
            "sa.product_id" +
            ", " +
            "CASE " +
            "   WHEN (vp.original_price IS NULL OR vp.original_price = 0 OR vp.original_price <= sa.seckill_price) " +
            "   THEN sa.seckill_price * 1.25 " +
            "   ELSE vp.original_price " +
            "END as original_price " +
            "FROM seckill_activity sa " +
            "LEFT JOIN vip_package vp ON sa.product_type = #{productTypeVip} AND sa.product_id = vp.id " +
            "WHERE sa.start_time > #{currentTime} AND sa.status = 0 " +
            "ORDER BY sa.start_time ASC")
    IPage<SeckillActivityVO> selectUpcomingSeckillActivities(Page<SeckillActivity> page,
                                                             @Param("currentTime") LocalDateTime currentTime,
                                                             @Param("productTypeVip") Integer productTypeVip);

    /**
     * 分页查询正在进行中的秒杀活动（时间范围内 + 已开始）
     * 核心逻辑：original_price = 0 或 ≤ seckill_price 时，设为 seckill_price * 1.25
     *
     * @param page           分页参数
     * @param currentTime    当前时间
     * @param productTypeVip 会员套餐产品类型编码
     * @return 秒杀活动VO分页结果
     */
    @Select("SELECT sa.id as activity_id," +
            "sa.activity_name," +
            "sa.total_stock," +
            "sa.remaining_stock," +
            "sa.seckill_price," +
            "sa.start_time," +
            "sa.end_time," +
            "sa.status," +
            "sa.product_type," +
            "sa.product_id" +
            ", " +
            "CASE " +
            "   WHEN (vp.original_price IS NULL OR vp.original_price = 0 OR vp.original_price <= sa.seckill_price) " +
            "   THEN sa.seckill_price * 1.25 " +
            "   ELSE vp.original_price " +
            "END as original_price " +
            "FROM seckill_activity sa " +
            "LEFT JOIN vip_package vp ON sa.product_type = #{productTypeVip} AND sa.product_id = vp.id " +
            "WHERE sa.start_time <= #{currentTime} AND sa.end_time >= #{currentTime}  " +
            "ORDER BY sa.start_time ASC")
    IPage<SeckillActivityVO> selectOngoingSeckillActivities(Page<SeckillActivity> page,
                                                            @Param("currentTime") LocalDateTime currentTime,
                                                            @Param("productTypeVip") Integer productTypeVip);

    /**
     * 批量扣减库存（扩展：高并发批量处理）
     * @param activityIds 活动ID列表
     * @param now 当前时间
     * @return 影响行数
     */
    @Update("<script>" +
            "UPDATE seckill_activity " +
            "SET remaining_stock = remaining_stock - 1, update_time = #{now} " +
            "WHERE id IN " +
            "<foreach collection='activityIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND remaining_stock > 0" +
            "</script>")
    int batchDecreaseStock(@Param("activityIds") Iterable<Long> activityIds,
                           @Param("now") LocalDateTime now);
}