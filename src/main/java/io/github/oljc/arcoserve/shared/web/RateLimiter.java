package io.github.oljc.arcoserve.shared.web;

import io.github.oljc.arcoserve.shared.annotation.RateLimit;
import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.Code;
import io.github.oljc.arcoserve.shared.util.AnnotationUtils;
import io.github.oljc.arcoserve.shared.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 限流拦截器
 * 基于Redis实现的滑动窗口限流算法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtils jwtUtils;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

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
        List<String> keys = buildRateLimitKeys(request, rateLimit);

        for (String key : keys) {
            if (isRateLimited(key, rateLimit.limit(), rateLimit.window())) {
                String message = StringUtils.hasText(rateLimit.message()) ?
                    rateLimit.message() : "访问过于频繁，请稍后再试";
                log.warn("Rate limit exceeded for key: {}, limit: {}, window: {}s",
                    key, rateLimit.limit(), rateLimit.window());
                throw new BusinessException(Code.TOO_MANY_REQUESTS, message);
            }
        }
    }

    private static final ThreadLocal<StringBuilder> stringBuilder = ThreadLocal.withInitial(() -> new StringBuilder(128));

    /**
     * 构建限流键列表
     * 使用哈希缩短key长度，提升性能
     */
    private List<String> buildRateLimitKeys(HttpServletRequest request, RateLimit rateLimit) {
        String uri = request.getRequestURI();
        StringBuilder sb = stringBuilder.get();
        sb.setLength(0);
        sb.append("rl:").append(uri);

        if (rateLimit.ip()) {
            String ip = request.getHeader("X-Real-IP");
            sb.append(":").append(ip);
        }

        if (rateLimit.device()) {
            String device = request.getHeader("X-Fingerprint");
            sb.append(":").append(device);
        }

        if (rateLimit.user()) {
            String userId = jwtUtils.getUserId(request);
            sb.append(":").append(userId);
        }

        String key = sb.toString();
        return Arrays.asList(key);
    }

    /**
     * 检查是否被限流
     * 使用滑动窗口算法
     */
    private boolean isRateLimited(String key, int limit, long windowInSeconds) {
        try {
            // 获取当前时间戳（秒）
            long currentTime = System.currentTimeMillis() / 1000;
            String windowKey = key + ":" + currentTime;

            // 使用Redis管道执行多个命令
            List<Object> results = redisTemplate.executePipelined(connection -> {
                connection.incr(windowKey.getBytes());
                connection.expire(windowKey.getBytes(), windowInSeconds);
                return null;
            });

            if (results != null && !results.isEmpty()) {
                Long count = (Long) results.get(0);
                return count != null && count > limit;
            }

            return false;
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // 发生异常时不限流，保证系统可用性
            return false;
        }
    }
}
