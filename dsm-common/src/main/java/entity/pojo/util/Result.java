package entity.pojo.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 泛型版统一返回结果类
 * @param <T> 数据字段(data)的类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Result<T> {

    private Boolean success;
    private String errorMsg;
    private T data;
    private Long total;
private String errCode;

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> ok() {
        return new Result<>(true, null, null, null, null);
    }

    /**
     * 成功返回（带单个数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, null, data, null, null);
    }

    /**
     * 成功返回（带列表数据和总数）
     */
    public static <T> Result<List<T>> ok(List<T> data, Long total) {
        return new Result<>(true, null, data, total, null);
    }

    /**
     * 失败返回（仅错误信息）
     */
    public static <T> Result<T> fail(String errorMsg) {
        return new Result<>(false, errorMsg, null, null, null);
    }

    /**
     * 失败返回（仅 String 错误码）
     */
    public static <T> Result<T> failByCode(String errCode) {
        return new Result<>(false, null, null, null, errCode);
    }

    /**
     * 失败返回（String 错误码 + 错误信息）→ 核心新增，适配自定义异常
     */
    public static <T> Result<T> fail(String errCode, String errorMsg) {
        return new Result<>(false, errorMsg, null, null, errCode);
    }

    // ------------------- 以下为兼容旧逻辑的 Integer 错误码方法（可选保留）-------------------
    /**
     * 失败返回（仅 Integer 错误码）→ 兼容旧代码，建议逐步淘汰
     */
    @Deprecated
    public static <T> Result<T> fail(Integer errCode) {
        return new Result<>(false, null, null, null, errCode != null ? errCode.toString() : null);
    }

    /**
     * 失败返回（Integer 错误码 + 错误信息）→ 兼容旧代码，建议逐步淘汰
     */
    @Deprecated
    public static <T> Result<T> fail(Integer errCode, String errorMsg) {
        return new Result<>(false, errorMsg, null, null, errCode != null ? errCode.toString() : null);
    }
}