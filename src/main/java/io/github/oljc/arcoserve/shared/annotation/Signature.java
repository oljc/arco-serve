package io.github.oljc.arcoserve.shared.annotation;

import java.lang.annotation.*;

/**
 * 签名验证注解
 *
 * 支持作用于类和方法，方法上的注解优先级高于类上的
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Signature {

    /**
     * 是否启用签名验证
     */
    boolean required() default true;

    /**
     * 签名的最大有效时间（秒）
     */
    long maxAge() default 300L;
}
