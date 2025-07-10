package io.github.oljc.arcoserve.shared.exception;

import io.github.oljc.arcoserve.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("参数验证失败: {}", ex.getMessage());

        List<ApiResponse.ErrorDetail> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildErrorDetail)
                .collect(Collectors.toList());

        ApiResponse<Void> response = ApiResponse.error(
                ResultCode.VALIDATION_ERROR.getCode(),
                ResultCode.VALIDATION_ERROR.getMessage(),
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("业务异常: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ex.getCode(), ex.getMessage());

        // 业务异常统一返回HTTP 200，错误信息通过响应体传递
        return ResponseEntity.ok(response);
    }

    /**
     * 参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("参数类型转换异常: {}", ex.getMessage());

        String message = String.format("参数 '%s' 类型错误，期望类型: %s",
                ex.getName(), ex.getRequiredType().getSimpleName());

        ApiResponse<Void> response = ApiResponse.error(
                ResultCode.BAD_REQUEST.getCode(),
                message
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 请求体读取异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("请求体读取异常: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ResultCode.BAD_REQUEST.getCode(),
                "请求体格式错误，请检查JSON格式"
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("404异常: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ResultCode.NOT_FOUND.getCode(),
                "请求的资源不存在"
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex, HttpServletRequest request) {

        log.error("系统异常: ", ex);

        ApiResponse<Void> response = ApiResponse.error(
                ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                ResultCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 构建错误详情
     */
    private ApiResponse.ErrorDetail buildErrorDetail(FieldError fieldError) {
        return new ApiResponse.ErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue(),
                "FIELD_ERROR"
        );
    }

    /**
     * 根据业务错误码获取HTTP状态码
     */
    private HttpStatus getHttpStatus(Integer code) {
        if (code >= 400 && code < 500) {
            return HttpStatus.valueOf(code);
        } else if (code >= 500 && code < 600) {
            return HttpStatus.valueOf(code);
        } else {
            // 业务错误码默认返回200
            return HttpStatus.OK;
        }
    }
}
