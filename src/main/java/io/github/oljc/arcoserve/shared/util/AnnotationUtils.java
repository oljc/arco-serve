package io.github.oljc.arcoserve.shared.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationUtils {

    // 缓存结构：key = 类名#方法签名 -> (注解类型 -> 注解实例)
    private static final Map<String, Map<Class<? extends Annotation>, Annotation>> cache = new ConcurrentHashMap<>();

    private static String buildKey(Method method, Class<?> clazz) {
        return clazz.getName() + "#" + method.toGenericString();
    }

    public static <A extends Annotation> A find(HandlerMethod handlerMethod, Class<A> annotationClass) {
        Method method = handlerMethod.getMethod();
        Class<?> beanType = handlerMethod.getBeanType();
        return findAnnotationWithCache(method, beanType, annotationClass);
    }

    @SuppressWarnings("unused")
    public static <A extends Annotation> A find(JoinPoint joinPoint, Class<A> annotationClass) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return findAnnotationWithCache(method, targetClass, annotationClass);
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> A findAnnotationWithCache(Method method, Class<?> clazz, Class<A> annotationClass) {
        String key = buildKey(method, clazz);

        return (A) cache
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(annotationClass, (Class<? extends Annotation> cls) -> {
                    A methodAnn = AnnotatedElementUtils.findMergedAnnotation(method, annotationClass);
                    if (methodAnn != null) return methodAnn;
                    return AnnotatedElementUtils.findMergedAnnotation(clazz, annotationClass);
                });
    }

    @SuppressWarnings("unused")
    public static void clearCache() {
        cache.clear();
    }
}
