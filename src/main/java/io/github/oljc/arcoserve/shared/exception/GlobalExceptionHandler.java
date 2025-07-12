package io.github.oljc.arcoserve.shared.exception;

import io.github.oljc.arcoserve.shared.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 自定义业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 参数校验失败 - @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage(),
                        (v1, v2) -> v1
                ));

        return ApiResponse.error(Code.VALIDATION_ERROR, errors);
    }

    /**
     * 参数绑定失败 - @ModelAttribute
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage(),
                        (v1, v2) -> v1
                ));

        return ApiResponse.error(Code.VALIDATION_ERROR, errors);
    }

    /**
     * 缺少参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiResponse.error(Code.BAD_REQUEST, Map.of("param", ex.getParameterName()));
    }

    /**
     * 请求体错误（如JSON格式错误）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return ApiResponse.error(Code.BAD_REQUEST, "请求体格式错误");
    }

    /**
     * 兜底
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        log.error("系统错误", ex);
        return ApiResponse.error(Code.ERROR);
    }
}
