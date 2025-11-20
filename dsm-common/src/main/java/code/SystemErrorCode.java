package code;

import lombok.AllArgsConstructor;

/**
 * 系统错误状态码（此类返回码应该高度重视）
 * <p>
 * <p>
 * /10/24 20:09
 */
// @Getter
@AllArgsConstructor
public enum SystemErrorCode implements ErrorCode {

    /**
     * 系统错误
     */
    SYSTEM_ERROR("10001", "系统似乎出现了点小问题"),

    ;

    private final String code;

    private final String msg;

    private final String level;

    SystemErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
        this.level = LEVEL_SYSTEM;
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
