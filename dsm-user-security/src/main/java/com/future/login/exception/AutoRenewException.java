package com.future.login.exception;

import lombok.Getter;

/**
 * VIP自动续费相关异常
 */
public class AutoRenewException extends BusinessException {

    // 1. 错误码前缀常量
    private static final String CODE_PREFIX = "AUTO_RENEW_";

    // 2. Extra格式模板常量（统一拼接规则，避免硬编码）
    private static final String EXTRA_USER_ID = "userId=%d";
    private static final String EXTRA_USER_VIP = "userId=%d, vipId=%d";

    // 3. 枚举：统一管理错误码和错误消息（核心常量）
    @Getter
    public enum AutoRenewError {
        LOCK_FAILED(CODE_PREFIX + "001", "自动续费并发冲突，请稍后重试"),
        PAYMENT_FAILED(CODE_PREFIX + "002", "自动续费支付失败"),
        STATUS_CHANGED(CODE_PREFIX + "003", "VIP状态已变更，无法执行自动续费");

        // getter
        private final String code;
        private final String message;

        AutoRenewError(String code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    // 4. 静态工厂方法（对外调用方式不变，兼容原有逻辑）
    // 锁竞争失败（并发续费）
    public static AutoRenewException lockFailed(Long userId) {
        AutoRenewError error = AutoRenewError.LOCK_FAILED;
        String extra = String.format(EXTRA_USER_ID, userId);
        return new AutoRenewException(error.getCode(), error.getMessage(), extra);
    }

    // 续费支付失败
    public static AutoRenewException paymentFailed(Long userId, Long vipId) {
        AutoRenewError error = AutoRenewError.PAYMENT_FAILED;
        String extra = String.format(EXTRA_USER_VIP, userId, vipId);
        return new AutoRenewException(error.getCode(), error.getMessage(), extra);
    }

    // 续费状态异常（如VIP已取消自动续费）
    public static AutoRenewException statusChanged(Long userId, Long vipId) {
        AutoRenewError error = AutoRenewError.STATUS_CHANGED;
        String extra = String.format(EXTRA_USER_VIP, userId, vipId);
        return new AutoRenewException(error.getCode(), error.getMessage(), extra);
    }

    // 构造方法（复用父类）
    public AutoRenewException(String code, String message) {
        super(code, message);
    }

    public AutoRenewException(String code, String message, String extra) {
        super(code, message, extra);
    }
}