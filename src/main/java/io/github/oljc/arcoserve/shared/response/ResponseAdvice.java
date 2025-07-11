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
 * 自动包装响应
 */
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> returnClass = returnType.getParameterType();

        if (ApiResponse.class.isAssignableFrom(returnClass)) return false;

        // 不处理系统类和特殊的响应
        String packageName = returnType.getDeclaringClass().getPackageName();
        return !packageName.startsWith("org.springframework") && !ResponseEntity.class.isAssignableFrom(returnClass);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        if (body instanceof String) {
            return JsonUtils.toJson(ApiResponse.success(body));
        }

        return ApiResponse.success(body);
    }
}
