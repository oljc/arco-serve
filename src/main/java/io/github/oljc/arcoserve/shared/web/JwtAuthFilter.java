package io.github.oljc.arcoserve.shared.web;

import io.github.oljc.arcoserve.modules.user.User;
import io.github.oljc.arcoserve.modules.user.UserService;
import io.github.oljc.arcoserve.shared.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器 - 增强安全版本
 *
 * 支持IP地址和设备指纹双重验证
 * Token从 access-token 请求头获取
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 提取当前请求的安全信息
                String currentIp = jwtUtils.extractIp(request);
                String currentFingerprint = jwtUtils.extractFingerprint(request);

                // 验证token（包括IP和设备指纹校验）
                if (jwtUtils.validate(token, currentIp, currentFingerprint)) {
                    authenticateUser(request, token);
                } else {
                    log.debug("JWT token validation failed for IP: {} and fingerprint present: {}",
                             currentIp, currentFingerprint != null);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            // 清理安全上下文，确保不会有残留的认证信息
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 认证用户并设置安全上下文
     */
    private void authenticateUser(HttpServletRequest request, String token) {
        try {
            // 从token中提取用户ID
            UUID userId = jwtUtils.getUserId(token);

            // 从数据库查询用户信息（确保信息是最新的）
            User user = userService.findById(userId).orElse(null);
            if (user == null || !user.isEnabled()) {
                log.warn("User not found or disabled for token: {}", userId);
                return;
            }

            // 获取用户权限
            Set<String> permissions = getUserPermissions(user);

            // 创建权限列表
            var authorities = permissions.stream()
                .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission))
                .collect(Collectors.toList());

            // 添加角色权限
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name()));

            // 创建认证对象
            var authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, authorities);

            // 设置认证详情
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 设置到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for user: {} with {} permissions",
                     user.getUsername(), permissions.size());

        } catch (Exception e) {
            log.error("Failed to authenticate user from JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 获取用户权限
     */
    private Set<String> getUserPermissions(User user) {
        Set<String> permissions = new HashSet<>();

        // 根据用户类型分配基础权限
        switch (user.getUserType()) {
            case USER -> {
                permissions.add("USER_READ");
                permissions.add("USER_WRITE");
            }
            case ADMIN -> {
                permissions.add("USER_READ");
                permissions.add("USER_WRITE");
                permissions.add("ADMIN_READ");
                permissions.add("ADMIN_WRITE");
                permissions.add("SYSTEM_MANAGE");
            }
            case MODERATOR -> {
                permissions.add("USER_READ");
                permissions.add("USER_WRITE");
                permissions.add("MODERATE_CONTENT");
            }
        }

        return permissions;
    }

    /**
     * 从请求中提取JWT token
     * 从 access-token 请求头获取token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("access-token");

        if (StringUtils.hasText(token)) {
            // 验证token格式
            if (isValidTokenFormat(token)) {
                return token;
            }
        }

        return null;
    }

    /**
     * 验证token格式是否正确
     */
    private boolean isValidTokenFormat(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        // JWT应该有两个点分隔的三个部分
        String[] parts = token.split("\\.");
        return parts.length == 3 &&
               parts[0].length() > 0 &&
               parts[1].length() > 0 &&
               parts[2].length() > 0;
    }

    /**
     * 跳过特定路径的认证
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/captcha/") ||
               path.equals("/error");
    }
}
