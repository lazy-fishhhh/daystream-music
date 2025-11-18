package com.future.login.handler;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

import static code.SystemErrorCode.SYSTEM_ERROR;
import static code.UserErrorCode.PARAM_ERROR;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 JSON 解析异常（比如前端传了非法 JSON）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleJsonParseException(HttpMessageNotReadableException e) {
        log.warn("JSON 解析异常: {}", e.getMessage());
        return Result.fail(PARAM_ERROR.getCode(), "参数 JSON 格式错误");
    }

    /**
     * 处理参数校验异常--针对 @RequestBody + @Valid 校验
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<String> errorMsgs = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        String message = "参数校验失败: " + String.join("; ", errorMsgs);
        log.warn(message);
        return Result.fail(PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理参数绑定异常 —— 针对 @ModelAttribute 或非 JSON 形式传参校验
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<String> errorMsgs = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        String message = "参数绑定失败: " + String.join("; ", errorMsgs);
        log.warn(message);
        return Result.fail(PARAM_ERROR.getCode(), message);
    }
    /**
     * 统一兜底：捕获所有其他未处理的异常，避免暴露敏感信息
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail(SYSTEM_ERROR.getCode(),"服务器出错啦~请联系技术人员排查");
    }
}