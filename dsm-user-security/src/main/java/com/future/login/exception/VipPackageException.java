package com.future.login.exception;

import lombok.Getter;

/**
 * VIP套餐相关业务异常
 */
public class VipPackageException extends BusinessException {

    // 1. 错误码前缀常量
    private static final String CODE_PREFIX = "VIP_PACKAGE_";

    // 2. Extra格式模板常量（统一拼接规则，避免硬编码）
    private static final String EXTRA_PACKAGE_NAME = "packageName=%s";
    private static final String EXTRA_PACKAGE_ID = "packageId=%d";
    private static final String EXTRA_PACKAGE_DURATION = "packageId=%d, durationDays=%d";

    // 3. 枚举：统一管理错误码和错误消息（核心常量）
    @Getter
    public enum VipPackageError {
        NOT_FOUND(CODE_PREFIX + "001", "VIP套餐不存在"),
        INVALID(CODE_PREFIX + "002", "VIP套餐已下架或无效"),
        INVALID_DURATION(CODE_PREFIX + "003", "VIP套餐时长非法");

        // getter
        private final String code;
        private final String message;

        VipPackageError(String code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    // 4. 静态工厂方法（对外调用方式不变，兼容原有逻辑）
    // 套餐不存在
    public static VipPackageException notFound(String packageName) {
        VipPackageError error = VipPackageError.NOT_FOUND;
        String extra = String.format(EXTRA_PACKAGE_NAME, packageName);
        return new VipPackageException(error.getCode(), error.getMessage(), extra);
    }

    // 套餐无效（如已下架）
    public static VipPackageException invalid(Long packageId) {
        VipPackageError error = VipPackageError.INVALID;
        String extra = String.format(EXTRA_PACKAGE_ID, packageId);
        return new VipPackageException(error.getCode(), error.getMessage(), extra);
    }

    // 套餐时长非法
    public static VipPackageException invalidDuration(Long packageId, Integer durationDays) {
        VipPackageError error = VipPackageError.INVALID_DURATION;
        String extra = String.format(EXTRA_PACKAGE_DURATION, packageId, durationDays);
        return new VipPackageException(error.getCode(), error.getMessage(), extra);
    }

    // 构造方法（复用父类）
    public VipPackageException(String code, String message) {
        super(code, message);
    }

    public VipPackageException(String code, String message, String extra) {
        super(code, message, extra);
    }
}