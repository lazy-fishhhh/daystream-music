package com.future.login.handler;
import com.future.login.exception.*;

import entity.pojo.util.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

import static code.SystemErrorCode.SYSTEM_ERROR;
import static code.UserErrorCode.PARAM_ERROR;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @RequestParam / @PathVariable 参数校验失败的异常
     * 例如：@NotNull, @Min, @Max 等直接作用在方法参数上的注解
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置HTTP响应状态码为 400 Bad Request
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        // 从异常中提取所有的错误提示信息，并用逗号拼接起来
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("，"));

        // 返回一个自定义的失败 Result 对象
        return Result.fail(errorMessage);
    }


    @ExceptionHandler(VipStatusException.class)
    public Result<?> handleVipStatusException(VipStatusException e) {
        log.warn("会员状态异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(VipPackageException.class)
    public Result<?> handleVipPackageException(VipPackageException e) {
        log.warn("会员套餐异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(OrderException.class)
    public Result<?> handleOrderException(OrderException e) {
        log.warn("订单异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }


    @ExceptionHandler(AutoRenewException.class)
    public Result<?> handleAutoRenewException(AutoRenewException e) {
        log.warn("自动续费异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("系统异常: ", e);
        return Result.fail(SYSTEM_ERROR.getCode(),"服务器出错啦~请联系技术人员排查");
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}，扩展信息: {}", e.getMessage(), e.getExtra());
        return Result.fail(e.getCode(), e.getMessage());
    }
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