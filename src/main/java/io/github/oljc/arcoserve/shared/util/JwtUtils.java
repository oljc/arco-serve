package io.github.oljc.arcoserve.shared.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类 - 双token方案
 */
@Component
public class JwtUtils {

    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final Duration accessExpiry;
    private final Duration refreshExpiry;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience,
            @Value("${app.jwt.access-token-expiration}") Duration accessExpiry,
            @Value("${app.jwt.refresh-token-expiration}") Duration refreshExpiry
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.audience = audience;
        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;
    }

    /**
     * 生成访问令牌
     */
    public String createAccessToken(Long userId, String username, String userType) {
        return createToken(userId, username, userType, accessExpiry, "access");
    }

    /**
     * 生成刷新令牌
     */
    public String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, null, refreshExpiry, "refresh");
    }

    /**
     * 生成令牌
     */
    private String createToken(Long userId, String username, String userType,
                              Duration expiry, String type) {
        var now = Instant.now();
        var exp = now.plus(expiry);

        var builder = Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())
                .claim("uid", userId)
                .claim("type", type);

        if (userType != null) {
            builder.claim("role", userType);
        }

        return builder.signWith(key).compact();
    }

    /**
     * 验证令牌
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析令牌
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        var uid = parse(token).get("uid");
        return uid instanceof Number number ? number.longValue() : null;
    }

    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    /**
     * 获取用户角色
     */
    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    /**
     * 获取令牌类型
     */
    public String getType(String token) {
        return parse(token).get("type", String.class);
    }

    /**
     * 获取令牌ID
     */
    public String getTokenId(String token) {
        return parse(token).getId();
    }

    /**
     * 检查是否过期
     */
    public boolean isExpired(String token) {
        try {
            return parse(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 获取过期时间
     */
    public Date getExpiry(String token) {
        return parse(token).getExpiration();
    }

    /**
     * 获取剩余秒数
     */
    public long getRemainSeconds(String token) {
        var exp = getExpiry(token);
        return Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
    }

    /**
     * 是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getType(token));
    }

    /**
     * 是否为访问令牌
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getType(token));
    }

    /**
     * 令牌信息
     */
    public record TokenInfo(
        String id,
        Long userId,
        String username,
        String role,
        String type,
        Date expiry,
        long remainSeconds
    ) {}

    /**
     * 获取令牌信息
     */
    public TokenInfo getInfo(String token) {
        var claims = parse(token);
        return new TokenInfo(
            claims.getId(),
            getUserId(token),
            claims.getSubject(),
            claims.get("role", String.class),
            claims.get("type", String.class),
            claims.getExpiration(),
            getRemainSeconds(token)
        );
    }
}
