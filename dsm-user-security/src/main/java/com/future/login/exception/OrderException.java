package com.future.login.exception;

import lombok.Getter;

/**
 * 订单相关业务异常
 */
public class OrderException extends BusinessException {

    // 1. 错误码前缀常量
    private static final String CODE_PREFIX = "ORDER_";

    // 2. 错误消息模板常量（避免字符串硬编码）
    private static final String EXTRA_ORDER_USER = "orderId=%d, userId=%d";
    private static final String EXTRA_ORDER_STATUS = "orderId=%d, currentStatus=%d, requiredStatus=%s";
    private static final String EXTRA_ORDER_ID = "orderId=%d";
    private static final String EXTRA_ORDER_REFUND_TIMEOUT = "orderId=%d, refundExpireTime=%s";

    // 3. 枚举：统一管理错误码和错误消息（核心常量）
    @Getter
    public enum OrderError {
        // 错误码（前缀+序号）→ 错误消息
        NOT_FOUND(CODE_PREFIX + "001", "订单不存在"),
        INVALID_STATUS(CODE_PREFIX + "002", "订单状态非法，不满足操作条件"),
        ITEM_NOT_FOUND(CODE_PREFIX + "003", "订单详情不存在"),
        ALREADY_REFUNDED(CODE_PREFIX + "004", "订单已退款，不可重复操作"),
        REFUND_TIMEOUT(CODE_PREFIX + "005", "订单超出退款时效"),
        UNKNOWN_ERROR(CODE_PREFIX + "999", "订单未知错误"),
        REFUND_FAIL(CODE_PREFIX + "006", "订单退款失败")

        ;
        // getter
        // 错误码
        private final String code;
        // 错误消息
        private final String message;

        OrderError(String code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    // 4. 静态工厂方法（对外暴露，用法不变）
    // 订单不存在
    public static OrderException notFound(Long orderId, Long userId) {
        OrderError error = OrderError.NOT_FOUND;
        String extra = String.format(EXTRA_ORDER_USER, orderId, userId);
        return new OrderException(error.getCode(), error.getMessage(), extra);
    }

    // 订单状态非法（如非已支付状态退款）
    public static OrderException invalidStatus(Long orderId, Integer currentStatus, String requiredStatus) {
        OrderError error = OrderError.INVALID_STATUS;
        String extra = String.format(EXTRA_ORDER_STATUS, orderId, currentStatus, requiredStatus);
        return new OrderException(error.getCode(), error.getMessage(), extra);
    }

    // 订单详情不存在
    public static OrderException itemNotFound(Long orderId) {
        OrderError error = OrderError.ITEM_NOT_FOUND;
        String extra = String.format(EXTRA_ORDER_ID, orderId);
        return new OrderException(error.getCode(), error.getMessage(), extra);
    }

    // 订单已退款
    public static OrderException alreadyRefunded(Long orderId) {
        OrderError error = OrderError.ALREADY_REFUNDED;
        String extra = String.format(EXTRA_ORDER_ID, orderId);
        return new OrderException(error.getCode(), error.getMessage(), extra);
    }

    // 订单超出退款时效
    public static OrderException refundTimeout(Long orderId, String expireTime) {
        OrderError error = OrderError.REFUND_TIMEOUT;
        String extra = String.format(EXTRA_ORDER_REFUND_TIMEOUT, orderId, expireTime);
        return new OrderException(error.getCode(), error.getMessage(), extra);
    }

    // 构造方法（复用父类）
    public OrderException(String code, String message) {
        super(code, message);
    }

    public OrderException(String code, String message, String extra) {
        super(code, message, extra);
    }
}