package constants;

/**
 * VIP业务核心常量（聚合套餐、会员相关枚举+基础配置）
 */
public class VipConstant {
    // 1. 套餐相关（类型+状态）
    public static class Package {
        // 套餐类型
        public static final String TYPE_BASIC = "BASIC";
        public static final String TYPE_PREMIUM = "PREMIUM";
        public static final String TYPE_SUPER = "SUPER";
        public static final String TYPE_BASIC_DESC = "基础套餐";
        public static final String TYPE_PREMIUM_DESC = "高级套餐";
        public static final String TYPE_SUPER_DESC = "超级套餐";

        // 套餐状态
        public static final int STATUS_OFFLINE = 0;
        public static final int STATUS_ONLINE = 1;
        public static final String STATUS_OFFLINE_DESC = "下架";
        public static final String STATUS_ONLINE_DESC = "上架";

        // 套餐基础配置
        public static final int PACKAGE_NAME_MAX_LENGTH = 100;
        public static final int DURATION_DAYS_MIN = 1;
    }

    // 2. 会员相关（状态+自动续费+来源）
    public static class Member {
        // 会员状态
        public static final String STATUS_ACTIVE = "ACTIVE";
        public static final String STATUS_EXPIRED = "EXPIRED";
        public static final String STATUS_CANCELLED = "CANCELLED";
        public static final String STATUS_ACTIVE_DESC = "生效中";
        public static final String STATUS_EXPIRED_DESC = "已过期";
        public static final String STATUS_CANCELLED_DESC = "已取消";

        // 自动续费
        public static final int AUTO_RENEW_DISABLE = 0;
        public static final int AUTO_RENEW_ENABLE = 1;
        public static final String AUTO_RENEW_DISABLE_DESC = "否";
        public static final String AUTO_RENEW_ENABLE_DESC = "是";

        // 会员来源
        public static final String SOURCE_PURCHASE = "PURCHASE";
        public static final String SOURCE_GIFT = "GIFT";
        public static final String SOURCE_PROMOTION = "PROMOTION";
        public static final String SOURCE_PURCHASE_DESC = "主动购买";
        public static final String SOURCE_GIFT_DESC = "他人赠送";
        public static final String SOURCE_PROMOTION_DESC = "活动赠送";

        // 会员基础配置
        public static final int LEVEL_MIN = 1;
        public static final int LEVEL_MAX = 3;
        public static final int PRICE_MIN = 0;
    }

    // 3. 表名常量
    public static final String TABLE_VIP_PACKAGE = "vip_package";
    public static final String TABLE_VIP = "vip";
}