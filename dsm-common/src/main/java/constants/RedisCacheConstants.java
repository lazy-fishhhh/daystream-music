package constants;

/**
 * Redis 缓存常量类（Key前缀 + 缓存配置）
 */
public class RedisCacheConstants {
    // ================================== 缓存时效常量（单位：秒） ==================================

    // 锁等待时间（3秒）
    public static final long LOCK_WAIT_TIME = 3L;
    /**
     * 缓存空值的过期时间（防缓存穿透），适用于查询无结果的场景（如不存在的用户、问卷）
     */
    public static final Long CACHE_NULL_TTL = 5L;

    /**
     * 分布式锁过期时间（防止死锁，需大于业务执行时间）
     */
    public static final Long LOCK_EXPIRE_TTL = 30L; // 30秒

    /**
     *
     */
    public static final Long ONE_MINUTE_TTL = 60L;

    /**
     * 短期缓存过期时间（适用于高频变动数据）
     */
    public static final Long CACHE_SHORT_TTL =30 * 60L; // 30分钟

    /**
     * 普通缓存过期时间（适用于中等变动数据）
     */
    public static final Long CACHE_NORMAL_TTL = 60 * 60L; // 60分钟

    /**
     * 长期缓存过期时间（适用于低频变动数据）
     */
    public static final Long CACHE_LONG_TTL = 24 * 60 * 60L; // 1天

    /**
     * 问卷缓存过期时间
     */
    public static final Long CACHE_QUESTIONNAIRE_TTL = 7 * 24 * 60 * 60L; // 7天

    /**
     * 分布式锁过期时间（防止死锁，需大于业务执行时间）
     */
//    public static final Long LOCK_EXPIRE_TTL = 30L; // 30秒

    /**

     缓存空值的随机过期时间（防缓存穿透，避免缓存同时失效引发缓存雪崩）
     适用于查询无结果的场景（如不存在的用户、问卷），随机值分散过期时间
     */
    public static final Long CACHE_NULL_TTL_RANDOM = (long) (5 + Math.random () * 5); // 5-10 秒随机值


    // ==================== 基础配置 ====================
    public static final String REDIS_SEPARATOR = ":"; // Key 分隔符
    public static final int DEFAULT_CACHE_TTL = 3600; // 默认缓存过期时间(秒)
    public static final int LONG_TERM_CACHE_TTL = 86400 * 7; // 长期缓存(7天)

    // ==================== 用户登录 Token 相关 ====================
    /** 用户登录 JWT Token 缓存前缀（存储 Token-用户信息映射） */
    public static final String TOKEN_USER_PREFIX = "token:user";
    /** 用户 Token 黑名单前缀（登出/封禁时失效 Token） */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist";
    /** Token 缓存过期时间（与 JWT 过期一致，2小时=7200秒） */
    public static final int TOKEN_CACHE_TTL = 7200;
    /** 黑名单 Token 过期时间（JWT 原有效期+1小时，防止复用） */
    public static final int TOKEN_BLACKLIST_TTL = 7200 + 3600;

    // ==================== 用户相关缓存 ====================
    //图形验证码缓存
    public static final String USER_CAPTCHA="user:captcha";
    //邮箱验证码缓存
    public static final String USER_EMAILCAPTCHA="user:emailCaptcha:";
    /** 用户信息缓存前缀（用户ID-用户详情） */
    public static final String USER_INFO_PREFIX = "user:info";
    /** 用户权限缓存前缀（用户ID-权限列表） */
    public static final String USER_PERMISSION_PREFIX = "user:permission";
    /** 用户签到缓存前缀（用户ID-年度签到记录） */
    public static final String USER_SIGN_IN_PREFIX = "user:signIn";
    /**用户收藏缓存前缀*/
    public static final String USER_COLLECTION="user:collect";
    /**用户点赞缓存前缀*/
    public static final String USER_LIKE="user:like";
    /**用户关注列表缓存*/
    public static final String USER_FOLLOW="user:follow";
    /**用户粉丝列表缓存*/
    public static final String USER_FOLLOWER="user:follower";


    // ==================== 音乐相关缓存 ====================
    /** 音乐元数据缓存前缀（音乐ID-元数据） */
    public static final String MUSIC_METADATA_PREFIX = "metadata";
    /** 热门歌曲缓存前缀（排序字段-歌曲列表） */
    public static final String MUSIC_HOT_PREFIX = "hot";
    /** 歌单缓存前缀（歌单ID-歌单详情+歌曲列表） */
    public static final String PLAYLIST_PREFIX = "playlist";

    // ==================== 交易与活动缓存 ====================
    /** VIP 秒杀活动库存前缀（活动ID-剩余库存） */
    public static final String SECKILL_STOCK_PREFIX = "seckill:stock";
    /** VIP 会员有效期缓存前缀（用户ID-到期时间） */
    public static final String VIP_EXPIRE_PREFIX = "vip:expire";
    /** 打赏记录缓存前缀（音乐人ID-最新打赏列表） */
    public static final String REWARD_RECORD_PREFIX = "reward:record";

    // ==================== 限流与防刷缓存 ====================
    /** 接口限流缓存前缀（接口名-IP/用户ID） */
    public static final String RATE_LIMIT_PREFIX = "rate:limit";
    /** 秒杀防刷缓存前缀（活动ID-IP） */
    public static final String SECKILL_ANTI_SPAM_PREFIX = "seckill:antispam";

    // ==================== 工具方法（拼接 Key） ====================
    /**
     * 拼接 Token-用户映射 Key
     * @param token JWT Token
     * @return 完整缓存 Key
     */
    public static String buildTokenUserKey(String token) {
        return TOKEN_USER_PREFIX + REDIS_SEPARATOR + token;
    }

    /**
     * 拼接 Token 黑名单 Key
     * @param token JWT Token
     * @return 完整缓存 Key
     */
    public static String buildTokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + REDIS_SEPARATOR + token;
    }

    public static final String LOCK_KEY_MUSIC_DETAIL = "lock:music:detail:";
}