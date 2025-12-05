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
    USER_INFO_ERROR("BUSINESS_ERROR_10005", "用户信息错误"),

    /**
     * 歌曲
     */
    MUSIC_NOT_EXIST("BUSINESS_ERROR_10002", "歌曲不存在"),
    PERSISTENCE_DENY("BUSINESS_ERROR_10006", "权限不足"),
    ;

    private final String code;

    private final String msg;

    BusinessErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
