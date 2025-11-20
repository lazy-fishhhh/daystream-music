package com.future.trade.constants;

/**
 * VIP Redis缓存前缀常量（单独一类，便于缓存管理）
 */
public class VipRedisConstant {
    // ====================================== 缓存前缀 ======================================
    // 订单缓存
    public static final String VIP_ORDER_INFO_PREFIX = "vip:order:info:";
    public static final String VIP_ORDER_LIST_PREFIX = "vip:order:list:";
    public static final String VIP_ORDER_COUNT_PREFIX = "vip:order:count:";
    public static final String VIP_ORDER_USER_LIST_PREFIX = "vip:order:user:list:";
    public static final String VIP_ORDER_USER_COUNT_PREFIX = "vip:order:user:count:";
    public static final String VIP_ORDER_USER_INFO_PREFIX = "vip:order:user:info:";

    // 套餐缓存
    public static final String VIP_PACKAGE_INFO_PREFIX = "vip:package:info:";
    public static final String VIP_PACKAGE_LIST_PREFIX = "vip:package:list:";

    // 用户VIP缓存
    public static final String USER_VIP_INFO_PREFIX = "vip:user:info:";
    public static final String USER_VIP_HISTORY_PREFIX = "vip:user:history:";
    public static final String USER_AUTO_RENEW_PREFIX = "vip:user:autorenew:";

    // 统计缓存
    public static final String PACKAGE_SALE_COUNT_PREFIX = "vip:package:sale:";
    public static final String LEVEL_USER_COUNT_PREFIX = "vip:level:count:";

    // ====================================== 缓存TTL（核心场景）======================================
    // 缓存空值TTL（秒）
    public static final Long CACHE_NULL_EXPIRE_TIME = 5L;

    /** 用户VIP信息缓存TTL：24小时（缓存用户生效中VIP信息，减少数据库查询） */
    public static final long VIP_CACHE_USER_INFO_24H = 24 * 60 * 60;

    /** VIP套餐列表缓存TTL：1小时（套餐信息变更频率低，缓存减轻查询压力） */
    public static final long VIP_CACHE_PACKAGE_LIST_1H = 60 * 60;

    /** VIP权益缓存TTL：6小时（权益配置变更少，缓存提升验证效率） */
    public static final long VIP_CACHE_BENEFIT_6H = 6 * 60 * 60;

    /** 支付回调订单锁定TTL：30分钟（防止重复回调处理，过期自动解锁） */
    public static final long VIP_PAY_CALLBACK_LOCK_30MIN = 30 * 60;

    /** VIP状态变更通知缓存TTL：12小时（存储通知记录，避免重复发送） */
    public static final long VIP_STATUS_NOTICE_CACHE_12H = 12 * 60 * 60;

    // ====================================== 业务有效期TTL（规则类）======================================
    /** VIP自动续费提醒提前期：7天（到期前7天开始发送续费提醒） */
    public static final long VIP_RENEWAL_REMIND_7DAY = 7 * 24 * 60 * 60;

    /** VIP过期后保留权益缓冲期：24小时（过期后24小时内仍可享受部分权益，引导续费） */
    public static final long VIP_EXPIRE_BUFFER_24H = 24 * 60 * 60;

    /** 促销活动VIP折扣有效期：3天（临时促销活动的折扣生效时长） */
    public static final long VIP_PROMOTION_DISCOUNT_3DAY = 3 * 24 * 60 * 60;

    /** 新用户首充VIP专属权益有效期：15天（新用户开通VIP后，额外专属权益的生效时长） */
    public static final long VIP_NEW_USER_BENEFIT_15DAY = 15 * 24 * 60 * 60;

    // ====================================== 临时缓存TTL（短期场景）======================================
    /** VIP等级升级计算缓存TTL：5分钟（升级所需条件计算结果，短期缓存避免重复计算） */
    public static final long VIP_LEVEL_UPGRADE_CALC_5MIN = 5 * 60;

    /** 验证码/临时凭证TTL：15分钟（VIP相关操作的验证码、临时授权凭证有效期） */
    public static final long VIP_TEMP_TOKEN_15MIN = 15 * 60;

    /** 并发操作锁TTL：1分钟（如VIP开通、续费等并发操作的分布式锁时长） */
    public static final long VIP_CONCURRENT_LOCK_1MIN = 60;


}