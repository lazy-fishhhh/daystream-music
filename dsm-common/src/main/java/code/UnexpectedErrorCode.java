package code;

import lombok.AllArgsConstructor;

/**
 * 未预期的错误码（即发生了不可能发生的事情，此类返回码应该高度重视）
 * <p>
 * <p>

 */
// @Getter
@AllArgsConstructor
public enum UnexpectedErrorCode implements ErrorCode {

    /**
     * 业务错误
     */
    BUSINESS_HANDING("20001", "呃~ 业务繁忙，请稍后重试"),

    /**
     * id错误
     */
    PAY_ORDER_ID_ERROR("20002", "付款单id发生了异常，请联系技术人员排查"),

    ;

    private final String code;

    private final String msg;

    private final String level;

    UnexpectedErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
        this.level = LEVEL_UNEXPECTED;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public String getLevel() {
        return level;
    }
}
