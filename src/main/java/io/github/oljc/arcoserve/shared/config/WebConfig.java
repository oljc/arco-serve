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
         registry.addMapping("/**")
                 .allowedOriginPatterns("*")
                 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                 .allowedHeaders("*")
                 .allowCredentials(true)
                 .maxAge(3600);
     }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimiter)
                .addPathPatterns("/**")
                .order(1);

        registry.addInterceptor(signInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }
}
