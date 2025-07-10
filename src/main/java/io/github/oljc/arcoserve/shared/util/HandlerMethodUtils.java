package io.github.oljc.arcoserve.shared.util;

import io.github.oljc.arcoserve.shared.annotation.Signature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HandlerMethod相关注解解析工具
 * 提供高性能的注解解析和缓存机制
 */
public final class HandlerMethodUtils {

    private static final String HANDLER_METHOD_ATTRIBUTE = "org.springframework.web.servlet.HandlerMapping.bestMatchingHandler";

    // 使用ConcurrentHashMap进行线程安全的缓存
    private static final ConcurrentHashMap<Method, Signature> METHOD_CACHE = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<Class<?>, Signature> CLASS_CACHE = new ConcurrentHashMap<>(64);

    // 私有构造函数，防止实例化
    private HandlerMethodUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 从请求上下文中获得HandlerMethod对应的Signature注解
     * 支持方法级优先，类级次之，支持组合注解
     * 使用缓存机制提升性能
     *
     * @param request 当前Http请求
     * @return Signature注解实例，找不到返回null
     */
    public static Signature getSignatureAnnotation(HttpServletRequest request) {
        var handler = request.getAttribute(HANDLER_METHOD_ATTRIBUTE);
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }

        return getSignatureAnnotation(handlerMethod);
    }

    /**
     * 从HandlerMethod中获取Signature注解
     *
     * @param handlerMethod HandlerMethod实例
     * @return Signature注解实例，找不到返回null
     */
    public static Signature getSignatureAnnotation(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }

        Method method = handlerMethod.getMethod();

        // 先检查方法级缓存
        Signature annotation = METHOD_CACHE.get(method);
        if (annotation != null) {
            return annotation;
        }

        // 方法级注解解析（支持组合注解）
        annotation = AnnotatedElementUtils.findMergedAnnotation(method, Signature.class);
        if (annotation != null) {
            METHOD_CACHE.put(method, annotation);
            return annotation;
        }

        // 类级注解解析
        Class<?> beanType = handlerMethod.getBeanType();
        annotation = CLASS_CACHE.computeIfAbsent(beanType,
            clazz -> AnnotatedElementUtils.findMergedAnnotation(clazz, Signature.class));

        // 即使是null也要缓存，避免重复解析
        if (annotation != null) {
            METHOD_CACHE.put(method, annotation);
        }

        return annotation;
    }

    /**
     * 清空缓存 - 主要用于测试或内存管理
     */
    public static void clearCache() {
        METHOD_CACHE.clear();
        CLASS_CACHE.clear();
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存大小信息
     */
    public static String getCacheStats() {
        return String.format("Method cache size: %d, Class cache size: %d",
            METHOD_CACHE.size(), CLASS_CACHE.size());
    }
}
