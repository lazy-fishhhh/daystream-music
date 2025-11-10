package com.example.music.constants;

/**
 * 积分、货币、交易相关常量
 */
public class TradeConstants {
    // 货币类型
    public static final String CURRENCY_TYPE_POINT = "POINT"; // 积分
    public static final String CURRENCY_TYPE_COIN = "COIN"; // 平台币

    // VIP套餐类型
    public static final String VIP_PACKAGE_MONTH = "MONTH"; // 月付
    public static final String VIP_PACKAGE_QUARTER = "QUARTER"; // 季付
    public static final String VIP_PACKAGE_YEAR = "YEAR"; // 年付
    public static final String VIP_PACKAGE_FREE_SEVEN = "FREE_SEVEN"; // 七天免费会员

    // 秒杀配置
    public static final int SECKILL_VIP_STOCK = 100; // 每周秒杀库存
    public static final String SECKILL_ACTIVITY_TIME = "SUNDAY 12:00:00"; // 秒杀活动时间

    // 分账规则
    public static final double PLATFORM_COMMISSION_RATE = 0.1; // 平台抽成比例(10%)

    // 积分获取途径
    public static final String POINT_SOURCE_SIGN_IN = "SIGN_IN"; // 签到
    public static final String POINT_SOURCE_RECHARGE = "RECHARGE"; // 充值
    public static final String POINT_SOURCE_REWARD = "REWARD"; // 打赏
}