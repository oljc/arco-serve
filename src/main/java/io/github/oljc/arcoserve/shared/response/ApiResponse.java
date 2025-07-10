package io.github.oljc.arcoserve.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.util.List;

/**
 * 统一API响应结构
 *
 * @param <T> 数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    /**
     * 响应状态: success | error
     */
    String status,

    /**
     * 状态码
     */
    Integer code,

    /**
     * 响应消息
     */
    String message,

    /**
     * 响应数据
     */
    T data,

    /**
     * 错误详情（仅在error时存在）
     */
    List<ErrorDetail> errors,

    /**
     * 请求追踪ID
     */
    String traceId,

    /**
     * 响应时间戳
     */
    Long timestamp
) {

    /**
     * 错误详情
     */
    public record ErrorDetail(
        String field,
        String message,
        Object rejectedValue,
        String code
    ) {}

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                "success",
                200,
                "操作成功",
                data,
                null,
                getCurrentTraceId(),
                System.currentTimeMillis()
        );
    }

    /**
     * 成功响应 - 自定义消息
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
                "success",
                200,
                message,
                data,
                null,
                getCurrentTraceId(),
                System.currentTimeMillis()
        );
    }

    /**
     * 成功响应 - 无数据
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(
                "success",
                200,
                "操作成功",
                null,
                null,
                getCurrentTraceId(),
                System.currentTimeMillis()
        );
    }

    /**
     * 成功响应 - 无数据，自定义消息
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(
                "success",
                200,
                message,
                null,
                null,
                getCurrentTraceId(),
                System.currentTimeMillis()
        );
    }

    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(
                "error",
                code,
                message,
                null,
                null,
                getCurrentTraceId(),
                System.currentTimeMillis()
        );
    }

    /**
     * 错误响应 - 带详情
     */
    public static <T> ApiResponse<T> error(Integer code, String message, List<ErrorDetail> errors) {
        return new ApiResponse<>(
                "error",
                code,
                message,
                null,
                errors,
                getCurrentTraceId(),
                System.currentTimeMillis()
        );
    }

    /**
     * 获取当前追踪ID
     */
    private static String getCurrentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : generateTraceId();
    }

    /**
     * 生成追踪ID
     */
    private static String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
