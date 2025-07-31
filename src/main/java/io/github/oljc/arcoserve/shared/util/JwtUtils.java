package io.github.oljc.arcoserve.shared.util;

import io.github.oljc.arcoserve.shared.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * JWT工具
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private static final String TOKEN_VERSION = "2.0";
    private static final String ACCESS_TYPE = "ACCESS";
    private static final String REFRESH_TYPE = "REFRESH";
    private static final String FINGERPRINT_HEADER = "X-Fingerprint";
    private static final String UNKNOWN_IP = "unknown";

    private static final String[] IP_HEADERS = {
        "X-Forwarded-For", "X-Real-IP", "X-Originating-IP",
        "CF-Connecting-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private static final Pattern IPv4_PATTERN = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private static final Pattern IPv6_PATTERN = Pattern.compile("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.issuer}")
    private String issuer;
    @Value("${app.jwt.audience}")
    private String audience;
    @Value("${app.jwt.access-token-expiration:PT15M}")
    private Duration accessExpiration;
    @Value("${app.jwt.refresh-token-expiration:P7D}")
    private Duration refreshExpiration;

    private final TokenBlacklistService blacklistService;

    private SecretKey signingKey;
    private JwtParser jwtParser;

    @PostConstruct
    private void init() {
        validateAndInitKey();
        initParser();
    }

    private void validateAndInitKey() {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT密钥长度必须至少32字节");
        }
        signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    }

    private void initParser() {
        jwtParser = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build();
    }

    // ==================== Token生成 ====================

    /**
     * 生成访问令牌
     */
    public String createAccessToken(UUID userId, String ip, String fingerprint) {
        validateSecurity(ip, fingerprint);
        return buildToken(userId, ip, fingerprint, ACCESS_TYPE, accessExpiration);
    }

    /**
     * 生成刷新令牌
     */
    public String createRefreshToken(UUID userId, String ip, String fingerprint) {
        validateSecurity(ip, fingerprint);
        return buildToken(userId, ip, fingerprint, REFRESH_TYPE, refreshExpiration);
    }

    private String buildToken(UUID userId, String ip, String fingerprint, String type, Duration expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(issuer)
            .subject(userId.toString())
            .audience().add(audience).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiration)))
            .id(UUID.randomUUID().toString())
            .claim("type", type)
            .claim("ip", ip)
            .claim("fingerprint", fingerprint)
            .claim("version", TOKEN_VERSION)
            .signWith(signingKey)
            .compact();
    }

    // ==================== Token验证 ====================

    /**
     * 完整验证（包含安全检查）
     */
    public boolean validate(String token, String currentIp, String currentFingerprint) {
        return parseAndValidate(token, claims ->
            validateSecurity(claims, currentIp, currentFingerprint));
    }

    /**
     * 基础验证（仅检查签名和有效期）
     */
    public boolean validateBasic(String token) {
        return parseAndValidate(token, claims -> true);
    }

    private boolean parseAndValidate(String token, SecurityValidator validator) {
        if (!StringUtils.hasText(token)) return false;

        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            return TOKEN_VERSION.equals(claims.get("version")) &&
                   !isBlacklisted(claims.getId()) &&
                   validator.validate(claims);

        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateSecurity(Claims claims, String currentIp, String currentFingerprint) {
        return currentIp.equals(claims.get("ip")) &&
               currentFingerprint.equals(claims.get("fingerprint"));
    }

    // ==================== Token操作 ====================

    /**
     * 刷新令牌对
     */
    public TokenPair refresh(String refreshToken, String ip, String fingerprint) {
        if (!validate(refreshToken, ip, fingerprint)) {
            throw new SecurityException("无效的刷新令牌");
        }

        Claims claims = parseClaims(refreshToken);
        if (!REFRESH_TYPE.equals(claims.get("type"))) {
            throw new SecurityException("令牌类型错误");
        }

        revoke(refreshToken);
        UUID userId = UUID.fromString(claims.getSubject());

        return new TokenPair(
            createAccessToken(userId, ip, fingerprint),
            createRefreshToken(userId, ip, fingerprint)
        );
    }

    /**
     * 撤销令牌
     */
    public void revoke(String token) {
        if (!StringUtils.hasText(token)) return;

        try {
            Claims claims = parseClaims(token);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();

            if (jti != null && expiration.after(new Date())) {
                Duration ttl = Duration.ofMillis(expiration.getTime() - System.currentTimeMillis());
                blacklistService.blacklistToken(jti, ttl);
            }
        } catch (Exception ignored) {}
    }

    // ==================== 信息提取 ====================

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getIp(String token) {
        return parseClaims(token).get("ip", String.class);
    }

    public String getFingerprint(String token) {
        return parseClaims(token).get("fingerprint", String.class);
    }

    public String getType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    // ==================== HTTP请求处理 ====================

    /**
     * 提取客户端IP
     */
    public String extractIp(HttpServletRequest request) {
        return java.util.Arrays.stream(IP_HEADERS)
            .map(request::getHeader)
            .filter(ip -> StringUtils.hasText(ip) && !UNKNOWN_IP.equalsIgnoreCase(ip))
            .map(this::getFirstIp)
            .filter(this::isValidIp)
            .findFirst()
            .orElseGet(() -> {
                String remoteAddr = request.getRemoteAddr();
                return isValidIp(remoteAddr) ? remoteAddr : UNKNOWN_IP;
            });
    }

    /**
     * 提取设备指纹
     */
    public String extractFingerprint(HttpServletRequest request) {
        String fingerprint = request.getHeader(FINGERPRINT_HEADER);
        return StringUtils.hasText(fingerprint) ? fingerprint.trim() : null;
    }

    // ==================== 私有辅助方法 ====================

    private Claims parseClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    private boolean isBlacklisted(String jti) {
        return StringUtils.hasText(jti) && blacklistService.isBlacklisted(jti);
    }

    private void validateSecurity(String ip, String fingerprint) {
        if (!StringUtils.hasText(ip) || UNKNOWN_IP.equals(ip)) {
            throw new SecurityException("无效的客户端IP");
        }
        if (!StringUtils.hasText(fingerprint)) {
            throw new SecurityException("缺少设备指纹");
        }
    }

    private String getFirstIp(String ipHeader) {
        return ipHeader.contains(",") ? ipHeader.split(",")[0].trim() : ipHeader;
    }

    private boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) &&
               !UNKNOWN_IP.equalsIgnoreCase(ip) &&
               (IPv4_PATTERN.matcher(ip).matches() ||
                IPv6_PATTERN.matcher(ip).matches() ||
                ip.contains(":"));
    }


    @FunctionalInterface
    private interface SecurityValidator {
        boolean validate(Claims claims);
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
