package com.future.login.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

/**
 * VIP状态相关业务异常
 */
@Slf4j
public class VipStatusException extends BusinessException {

    // 1. 错误码前缀常量
    private static final String CODE_PREFIX = "VIP_STATUS_";

    // 2. 模板常量（日志模板 + Extra格式模板）
    private static final String LOG_RECORD_NOT_FOUND = "获取用户{}的VIP状态失败，用户VIP记录不存在";
    private static final String LOG_EXPIRED = "获取用户{}的VIP状态失败，VIP已过期，过期时间为{}";
    private static final String LOG_INVALID_STATUS = "获取用户{}的VIP状态失败，VIP状态无效（如已取消）";

    private static final String EXTRA_USER_ID = "userId=%d";
    private static final String EXTRA_USER_EXPIRE = "userId=%d, expireTime=%s";

    // 3. 枚举：统一管理错误码和错误消息
    @Getter
    public enum VipStatusError {
        RECORD_NOT_FOUND(CODE_PREFIX + "001", "用户VIP记录不存在"),
        EXPIRED(CODE_PREFIX + "002", "VIP已过期"),
        INVALID_STATUS(CODE_PREFIX + "003", "获取VIP状态失败");

        // getter
        private final String code;
        private final String message;

        VipStatusError(String code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    // 4. 静态工厂方法（对外调用方式不变）
    // VIP记录不存在
    public static VipStatusException recordNotFound(Long userId) {
        VipStatusError error = VipStatusError.RECORD_NOT_FOUND;
        String extra = String.format(EXTRA_USER_ID, userId);
        log.error(LOG_RECORD_NOT_FOUND, userId); // 复用日志模板
        return new VipStatusException(error.getCode(), error.getMessage(), extra);
    }

    // VIP已过期
    public static VipStatusException expired(Long userId, LocalDateTime expireTime) {
        VipStatusError error = VipStatusError.EXPIRED;
        String extra = String.format(EXTRA_USER_EXPIRE, userId, expireTime);
        log.error(LOG_EXPIRED, userId, expireTime); // 复用日志模板
        return new VipStatusException(error.getCode(), error.getMessage(), extra);
    }

    // VIP状态无效（如已取消）
    public static VipStatusException invalidStatus(Long userId) {
        VipStatusError error = VipStatusError.INVALID_STATUS;
        String extra = String.format(EXTRA_USER_ID, userId);
        log.error(LOG_INVALID_STATUS, userId); // 修复原日志描述错误（原日志写"已过期"，实际是"状态无效"）
        return new VipStatusException(error.getCode(), error.getMessage(), extra);
    }

    // 构造方法（复用父类）
    public VipStatusException(String code, String message) {
        super(code, message);
    }

    public VipStatusException(String code, String message, String extra) {
        super(code, message, extra);
    }
}