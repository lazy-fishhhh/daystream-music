package com.future.login.exception;

import lombok.Getter;

/**
 * 业务异常根类
 */
@Getter
public class BusinessException extends RuntimeException {
    // 错误码
    private final String code;
    // 扩展字段（如用户ID、订单ID，用于日志排查）
    private final String extra;

    public BusinessException(String code, String message) {
        this(code, message, null);
    }

    public BusinessException(String message) {
        super(message);
        this.code = null;
        this.extra = null;
    }

    public BusinessException(String code, String message, String extra) {
        super(message);
        this.code = code;
        this.extra = extra;
    }
}