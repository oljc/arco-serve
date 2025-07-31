package io.github.oljc.arcoserve.shared.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token黑名单服务
 * 用于管理已撤销但尚未过期的JWT令牌
 */
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将令牌加入黑名单
     *
     * @param jti 令牌ID
     * @param ttl 剩余有效期
     */
    public void blacklistToken(String jti, Duration ttl) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl);
    }

    /**
     * 检查令牌是否在黑名单中
     *
     * @param jti 令牌ID
     * @return 如果令牌在黑名单中返回true
     */
    public boolean isBlacklisted(String jti) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
        return exists != null && exists;
    }
}