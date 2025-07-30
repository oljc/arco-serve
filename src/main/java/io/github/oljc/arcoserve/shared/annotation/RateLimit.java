package io.github.oljc.arcoserve.shared.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流次数，默认10次
     */
    int limit() default 10;

    /**
     * 时间窗口大小，单位：秒，默认60秒
     */
    long window() default 60;

    /**
     * 开启 IP 限流
     */
    boolean ip() default true;

    /**
     * 开启设备限流
     */
    boolean device() default true;

    /**
     * 开启用户限流
     */
    boolean user() default false;

    /**
     * 限流提示信息
     */
    String message() default "访问过于频繁，请稍后再试";
}
