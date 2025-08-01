package io.github.oljc.arcoserve.shared.web;

import io.github.oljc.arcoserve.shared.annotation.RateLimit;
import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.Code;
import io.github.oljc.arcoserve.shared.util.AnnotationUtils;
import io.github.oljc.arcoserve.shared.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 限流拦截器
 */
@Component
@RequiredArgsConstructor
public class RateLimiter implements HandlerInterceptor {

    private final RedisUtils redisUtils;
    private static final String HEADER_FP = "X-Fingerprint";
    private static final String HEADER_IP = "X-Real-IP";
    private static final String HEADER_TOKEN = "access-token";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimit rateLimit = AnnotationUtils.find(handlerMethod, RateLimit.class);
            if (rateLimit != null) {
                checkRateLimit(request, rateLimit);
            }
        }
        return true;
    }

    /**
     * 检查限流
     */
    private void checkRateLimit(HttpServletRequest request, RateLimit rateLimit) {
        String key = buildLimitKey(request, rateLimit);

        RedisUtils.RateLimitResult result = redisUtils.slidingWindowLimit(
            key,
            rateLimit.window(),
            rateLimit.limit()
        );

        if (!result.allowed()) {
            throw new BusinessException(Code.TOO_MANY_REQUESTS, rateLimit.message());
        }
    }

    /**
     * 构建限流键
     */
    private String buildLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        String path = request.getRequestURI();
        StringBuilder keyBuilder = new StringBuilder(128);

        keyBuilder.append(path);

        if (rateLimit.ip()) {
            keyBuilder.append(":").append(request.getHeader(HEADER_IP));
        }

        if (rateLimit.device()) {
            keyBuilder.append(":").append(request.getHeader(HEADER_FP));
        }

        if (rateLimit.user()) {
            keyBuilder.append(":").append(request.getHeader(HEADER_TOKEN));
        }

        return "rl:" + DigestUtils.md5DigestAsHex(keyBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
