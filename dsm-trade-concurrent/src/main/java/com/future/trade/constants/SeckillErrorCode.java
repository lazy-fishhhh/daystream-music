package com.future.trade.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 秒杀功能错误码（避免硬编码）
 */
@Getter
@AllArgsConstructor
public enum SeckillErrorCode {
    // 通用错误
    PARAM_ERROR("SECKILL_400", "参数非法"),
    SYSTEM_ERROR("SECKILL_500", "系统异常，请稍后重试"),

    // 秒杀活动相关错误
    ACTIVITY_NAME_DUPLICATE("SECKILL_4001", "活动名称已存在，请更换"),
    TIME_LOGIC_ERROR("SECKILL_4002", "活动时间逻辑错误（结束时间需晚于开始时间，且时长≤7天）"),
    STOCK_OUT_OF_RANGE("SECKILL_4003", "总库存需在1-100000之间"),
    PRICE_ILLEGAL("SECKILL_4004", "秒杀价格不能为负数"),
    PRODUCT_NOT_FOUND("SECKILL_4005", "关联商品不存在或已下架"),
    PRODUCT_TYPE_INVALID("SECKILL_4006", "商品类型无效（仅支持1-会员套餐、2-实物商品、3-虚拟商品）"),
    NOT_SECKILL_PACKAGE("SECKILL_4007", "会员套餐需选择「秒杀专属套餐」（package_type=SECKILL）"),
    // 秒杀活动状态相关错误
    ACTIVITY_NOT_FOUND("SECKILL_4008", "秒杀活动不存在"),
    ACTIVITY_NOT_STARTED("SECKILL_4009", "秒杀活动未开始"),
    ACTIVITY_ENDED("SECKILL_4010", "秒杀活动已结束"),
    ACTIVITY_CLOSED("SECKILL_4011", "秒杀活动已关闭"),
    ACTIVITY_FULL("SECKILL_4012", "秒杀活动已售罄"),

    // 秒杀订单相关错误
    ORDER_ALREADY_EXISTS("SECKILL_4013", "您已秒杀过该商品"),
    ORDER_STOCK_NOT_ENOUGH("SECKILL_4014", "秒杀商品库存不足"),
    ORDER_CREATE_FAILED("SECKILL_4015", "秒杀订单创建失败，请稍后重试");

    private final String code;
    private final String message;
}