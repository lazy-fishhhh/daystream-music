package com.future.login.exception;


/**
 * VIP套餐相关业务异常
 */
public class VipPackageException extends BusinessException {
    private static final String CODE_PREFIX = "VIP_PACKAGE_";

    // 套餐不存在
    public static VipPackageException notFound(String packageName) {
        String extra = String.format("packageName=%s", packageName);
        return new VipPackageException(CODE_PREFIX + "001", "VIP套餐不存在", extra);
    }

    // 套餐无效（如已下架）
    public static VipPackageException invalid(Long packageId) {
        return new VipPackageException(CODE_PREFIX + "002", "VIP套餐已下架或无效", "packageId=" + packageId);
    }

    // 套餐时长非法
    public static VipPackageException invalidDuration(Long packageId, Integer durationDays) {
        String extra = String.format("packageId=%d, durationDays=%d", packageId, durationDays);
        return new VipPackageException(CODE_PREFIX + "003", "VIP套餐时长非法", extra);
    }

    public VipPackageException(String code, String message) {
        super(code, message);
    }

    public VipPackageException(String code, String message, String extra) {
        super(code, message, extra);
    }
}