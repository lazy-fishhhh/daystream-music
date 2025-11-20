package com.future.login.exception;


/**
 * VIP自动续费相关异常
 */
public class AutoRenewException extends BusinessException {
    private static final String CODE_PREFIX = "AUTO_RENEW_";

    // 锁竞争失败（并发续费）
    public static AutoRenewException lockFailed(Long userId) {
        return new AutoRenewException(CODE_PREFIX + "001", "自动续费并发冲突，请稍后重试", "userId=" + userId);
    }

    // 续费支付失败
    public static AutoRenewException paymentFailed(Long userId, Long vipId) {
        String extra = String.format("userId=%d, vipId=%d", userId, vipId);
        return new AutoRenewException(CODE_PREFIX + "002", "自动续费支付失败", extra);
    }

    // 续费状态异常（如VIP已取消自动续费）
    public static AutoRenewException statusChanged(Long userId, Long vipId) {
        String extra = String.format("userId=%d, vipId=%d", userId, vipId);
        return new AutoRenewException(CODE_PREFIX + "003", "VIP状态已变更，无法执行自动续费", extra);
    }

    public AutoRenewException(String code, String message) {
        super(code, message);
    }

    public AutoRenewException(String code, String message, String extra) {
        super(code, message, extra);
    }
}