package io.github.oljc.arcoserve.shared.service;

import io.github.oljc.arcoserve.shared.util.JwtUtils;
import io.github.oljc.arcoserve.shared.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Token黑名单服务
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisUtils redisUtils;
    private final JwtUtils jwtUtils;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKENS_PREFIX = "jwt:user:";

    /**
     * 将token加入黑名单
     */
    public void addToBlacklist(String token) {
        try {
            if (!jwtUtils.isValid(token)) return;

            var tokenId = jwtUtils.getTokenId(token);
            var remainSeconds = jwtUtils.getRemainSeconds(token);

            if (remainSeconds > 0) {
                redisUtils.set(BLACKLIST_PREFIX + tokenId, "1", remainSeconds);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 检查token是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        try {
            var tokenId = jwtUtils.getTokenId(token);
            return redisUtils.exists(BLACKLIST_PREFIX + tokenId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将用户的所有token加入黑名单 (强制登出)
     */
    public void blacklistUserTokens(Long userId) {
        var userKey = USER_TOKENS_PREFIX + userId;
        var tokenIds = redisUtils.smembers(userKey);

        if (!tokenIds.isEmpty()) {
            tokenIds.forEach(tokenId ->
                redisUtils.set(BLACKLIST_PREFIX + tokenId, "1", 7 * 24 * 3600)
            );
            redisUtils.delete(userKey);
        }
    }

    /**
     * 记录用户的活跃token
     */
    public void trackUserToken(String token) {
        try {
            var userId = jwtUtils.getUserId(token);
            var tokenId = jwtUtils.getTokenId(token);
            var remainSeconds = jwtUtils.getRemainSeconds(token);

            if (userId != null && remainSeconds > 0) {
                var userKey = USER_TOKENS_PREFIX + userId;
                redisUtils.sadd(userKey, tokenId);
                redisUtils.expire(userKey, Math.max(remainSeconds, 7 * 24 * 3600));
            }
        } catch (Exception ignored) {}
    }

    /**
     * 移除用户token记录
     */
    public void untrackUserToken(String token) {
        try {
            var userId = jwtUtils.getUserId(token);
            var tokenId = jwtUtils.getTokenId(token);

            if (userId != null) {
                redisUtils.srem(USER_TOKENS_PREFIX + userId, tokenId);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 批量添加到黑名单
     */
    public void addToBlacklist(Collection<String> tokens) {
        tokens.forEach(this::addToBlacklist);
    }

    /**
     * 检查token是否有效
     */
    public boolean isTokenValid(String token) {
        return jwtUtils.isValid(token) && !isBlacklisted(token);
    }

    /**
     * 获取用户当前活跃的token数量
     */
    public long getUserActiveTokenCount(Long userId) {
        return redisUtils.scard(USER_TOKENS_PREFIX + userId);
    }

    /**
     * 清理过期的用户token记录
     */
    public void cleanupExpiredTokens(Long userId) {
        var userKey = USER_TOKENS_PREFIX + userId;
        var tokenIds = redisUtils.smembers(userKey);

        tokenIds.forEach(tokenId -> {
            if (!redisUtils.exists(BLACKLIST_PREFIX + tokenId)) {
                redisUtils.srem(userKey, tokenId);
            }
        });
    }
}
