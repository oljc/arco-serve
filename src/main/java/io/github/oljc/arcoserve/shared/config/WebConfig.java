package io.github.oljc.arcoserve.shared.config;

import io.github.oljc.arcoserve.shared.web.RateLimiter;
import io.github.oljc.arcoserve.shared.web.SignInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SignInterceptor signInterceptor;
    private final RateLimiter rateLimiter;

    public WebConfig(SignInterceptor signInterceptor, RateLimiter rateLimiter) {
        this.signInterceptor = signInterceptor;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器 - 优先级最高
        registry.addInterceptor(rateLimiter)
                .addPathPatterns("/api/**")
                .order(1);

        // 签名拦截器
        registry.addInterceptor(signInterceptor)
                .addPathPatterns("/api/**")
                .order(2);
    }
}
