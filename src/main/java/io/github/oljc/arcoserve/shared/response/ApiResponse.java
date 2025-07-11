package io.github.oljc.arcoserve.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.oljc.arcoserve.shared.exception.Code;
import org.slf4j.MDC;

/**
 * 统一 API 响应结构
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,
        Integer code,
        String message,
        T data,
        Object error,
        String traceId,
        Long timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return of("success", 200, "操作成功", data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return of("success", 200, message, data, null);
    }

    public static ApiResponse<Void> success() {
        return of("success", 200, "操作成功", null, null);
    }

    public static <T> ApiResponse<T> error(Code code) {
        return of("error", code.getCode(), code.getMessage(), null, null);
    }

    public static <T> ApiResponse<T> error(Code code, Object error) {
        return of("error", code.getCode(), code.getMessage(), null, error);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return of("error", code, message, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, Object error) {
        return of("error", code, message, null, error);
    }

    private static <T> ApiResponse<T> of(String status, int code, String message, T data, Object error) {
        return new ApiResponse<>(
                status,
                code,
                message,
                data,
                error,
                MDC.get("traceId"),
                System.currentTimeMillis()
        );
    }
}
