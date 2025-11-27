package code;

import lombok.Data;
import lombok.Getter;

@Getter
public enum BusinessErrorCode  {

    /**
     * 余额不足
     */
    BALANCE_NOT_ENOUGH("BUSINESS_ERROR_10001", "余额不足"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND("BUSINESS_ERROR_10002", "用户不存在"),
    PARAM_ERROR("BUSINESS_ERROR_10003", "参数错误"),
    BALANCE_NOT_ENOUGH_2("BUSINESS_ERROR_10004", "余额不足"),
    ;

    private final String code;

    private final String msg;

    BusinessErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
