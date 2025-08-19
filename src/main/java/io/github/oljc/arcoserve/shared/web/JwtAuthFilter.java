package io.github.oljc.arcoserve.shared.web;

import io.github.oljc.arcoserve.shared.service.TokenBlacklistService;
import io.github.oljc.arcoserve.shared.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;

    // Access Token 传输方式
    private static final String ACCESS_TOKEN_HEADER = "X-Access-Token";
    private static final String FALLBACK_HEADER = "access-token"; // 向后兼容

    // Refresh Token 传输方式
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 跳过已认证的请求
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 尝试从Access Token认证
            String accessToken = extractAccessToken(request);
            if (accessToken != null && validateAccessToken(accessToken)) {
                setAuthentication(request, accessToken);
                filterChain.doFilter(request, response);
                return;
            }

            // 如果是刷新token的端点，尝试从Refresh Token认证
            if (isRefreshEndpoint(request)) {
                String refreshToken = extractRefreshToken(request);
                if (refreshToken != null && validateRefreshToken(refreshToken)) {
                    setAuthentication(request, refreshToken);
                }
            }

        } catch (Exception e) {
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 提取Access Token
     */
    private String extractAccessToken(HttpServletRequest request) {
        // 优先从标准header提取
        String token = request.getHeader(ACCESS_TOKEN_HEADER);
        if (StringUtils.hasText(token)) {
            return token;
        }

        // 向后兼容旧header
        String fallback = request.getHeader(FALLBACK_HEADER);
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }

        return null;
    }

    /**
     * 提取Refresh Token (从HttpOnly Cookie)
     */
    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 验证Access Token
     */
    private boolean validateAccessToken(String token) {
        if (!jwtUtils.isValid(token)) {
            return false;
        }

        if (!jwtUtils.isAccessToken(token)) {
            return false;
        }

        if (tokenBlacklistService.isBlacklisted(token)) {
            return false;
        }

        // 记录活跃token
        tokenBlacklistService.trackUserToken(token);
        return true;
    }

    /**
     * 验证Refresh Token
     */
    private boolean validateRefreshToken(String token) {
        if (!jwtUtils.isValid(token)) {
            return false;
        }

        if (!jwtUtils.isRefreshToken(token)) {
            return false;
        }

        if (tokenBlacklistService.isBlacklisted(token)) {
            return false;
        }

        return true;
    }

    /**
     * 设置Spring Security认证信息
     */
    private void setAuthentication(HttpServletRequest request, String token) {
        var userId = jwtUtils.getUserId(token);
        var username = jwtUtils.getUsername(token);
        var role = jwtUtils.getRole(token);
        var tokenType = jwtUtils.getType(token);

        // 构建权限列表
        var authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
        );

        // 创建认证对象
        var authentication = new UsernamePasswordAuthenticationToken(
            new JwtUserPrincipal(userId, username, role, tokenType),
            null,
            authorities
        );

        // 设置请求详情
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 设置到安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 检查是否为刷新token端点
     */
    private boolean isRefreshEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/refresh".equals(path);
    }

    /**
     * JWT用户主体信息
     */
    public record JwtUserPrincipal(
        Long userId,
        String username,
        String role,
        String tokenType
    ) {
        @Override
        public String toString() {
            return username;
        }
    }

    /**
     * 跳过不需要认证的请求
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/captcha") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/actuator/") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/static/");
    }
}
