package io.github.oljc.arcoserve.shared.response;

import io.github.oljc.arcoserve.shared.util.JsonUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应统一包装处理器
 */
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> parameterType = returnType.getParameterType();

        // 不包装ApiResponse类型的响应
        if (ApiResponse.class.isAssignableFrom(parameterType)) {
            return false;
        }

        // 不包装ResponseEntity类型的响应（通常来自异常处理器）
        if (ResponseEntity.class.isAssignableFrom(parameterType)) {
            return false;
        }

        // 不包装Spring框架的响应
        if (returnType.getDeclaringClass().getName().startsWith("org.springframework") ||
            returnType.getDeclaringClass().getName().startsWith("springfox")) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        if (body instanceof String) {
            return JsonUtils.toJson(ApiResponse.success(body));
        }

        return ApiResponse.success(body);
    }
}
