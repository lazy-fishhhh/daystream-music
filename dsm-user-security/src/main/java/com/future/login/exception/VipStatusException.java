package com.future.login.exception;



import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * VIP状态相关业务异常
 */
@Slf4j
public class VipStatusException extends BusinessException {
    private static final String CODE_PREFIX = "VIP_STATUS_";

    // VIP记录不存在
    public static VipStatusException recordNotFound(Long userId) {
        log.error("获取用户{}的VIP状态失败，用户VIP记录不存在", userId);
        return new VipStatusException(CODE_PREFIX + "001", "用户VIP记录不存在", "userId=" + userId);
    }

    // VIP已过期
    public static VipStatusException expired(Long userId, LocalDateTime expireTime) {
        String extra = String.format("userId=%d, expireTime=%s", userId, expireTime);
        log.error("获取用户{}的VIP状态失败，VIP已过期，过期时间为{}", userId, expireTime);
        return new VipStatusException(CODE_PREFIX + "002", "VIP已过期", extra);
    }

    // VIP状态无效（如已取消）
    public static VipStatusException invalidStatus(Long userId) {
        String extra = String.format("userId=%d", userId);
        log.error("获取用户{}的VIP状态失败，VIP已过期", userId);
        return new VipStatusException(CODE_PREFIX + "003", "获取VIP状态失败", extra);
    }


    public VipStatusException(String code, String message) {
        super(code, message);
    }

    public VipStatusException(String code, String message, String extra) {
        super(code, message, extra);
    }
}