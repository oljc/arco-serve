package io.github.oljc.arcoserve.modules.auth;

import io.github.oljc.arcoserve.modules.user.User;
import io.github.oljc.arcoserve.modules.user.UserService;
import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.Code;
import io.github.oljc.arcoserve.shared.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * 认证服务 - 增强安全版本
 *
 * 支持IP地址和设备指纹双重验证的认证服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final CaptchaService captchaService;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @param request HTTP请求对象
     * @return 认证响应
     */
    @Transactional
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest loginRequest, HttpServletRequest request) {
        log.info("用户登录尝试: {}", loginRequest.usernameOrEmail());

        try {
            // 1. 验证验证码
            validateCaptcha(loginRequest.captchaId(), loginRequest.captchaCode());

            // 2. 提取请求安全信息
            String clientIp = jwtUtils.extractIp(request);
            String deviceFingerprint = jwtUtils.extractFingerprint(request);

            if (deviceFingerprint == null) {
                throw new BusinessException(Code.DEVICE_FINGERPRINT_REQUIRED);
            }

            // 3. 查找用户
            User user = findUserByUsernameOrEmail(loginRequest.usernameOrEmail());

            // 4. 验证用户状态
            validateUserStatus(user);

            // 5. 验证密码
            if (!userService.verifyPassword(user, loginRequest.password())) {
                // 记录登录失败（实际项目中应该更新登录尝试次数）
                log.warn("用户 {} 密码验证失败, IP: {}", user.getUsername(), clientIp);
                throw new BusinessException(Code.LOGIN_FAILED);
            }

            // 6. 获取用户权限（注释掉，因为新JWT不存储权限信息）
            // Set<String> permissions = getUserPermissions(user);

            // 7. 生成JWT token对
            String accessToken = jwtUtils.createAccessToken(
                user.getId(),
                clientIp,
                deviceFingerprint
            );

            String refreshToken = jwtUtils.createRefreshToken(
                user.getId(),
                clientIp,
                deviceFingerprint
            );

            // 8. 更新用户登录信息（实际项目中应该更新最后登录时间等）
            log.info("用户 {} 登录成功, IP: {}", user.getUsername(), clientIp);

            // 9. 计算token过期时间（秒）
            long expiresIn = Duration.ofMinutes(15).toSeconds(); // 15分钟

            return AuthDTO.AuthResponse.of(accessToken, refreshToken, expiresIn, user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录处理异常", e);
            throw new BusinessException(Code.SYSTEM_ERROR);
        }
    }

    /**
     * 刷新Token
     *
     * @param refreshRequest 刷新请求
     * @param request HTTP请求对象
     * @return Token响应
     */
    @Transactional
    public AuthDTO.TokenResponse refreshToken(AuthDTO.RefreshTokenRequest refreshRequest,
                                             HttpServletRequest request) {
        log.debug("Token刷新请求");

        try {
            // 1. 提取当前请求的安全信息
            String currentIp = jwtUtils.extractIp(request);
            String currentFingerprint = jwtUtils.extractFingerprint(request);

            if (currentFingerprint == null) {
                throw new BusinessException(Code.DEVICE_FINGERPRINT_REQUIRED);
            }

            // 2. 获取用户ID并验证用户状态
            String refreshToken = refreshRequest.refreshToken();
            UUID userId = jwtUtils.getUserId(refreshToken);
            User user = userService.findById(userId)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

            validateUserStatus(user);

            // 3. 刷新token对（新的JWT不再存储用户权限信息）
            JwtUtils.TokenPair tokenPair = jwtUtils.refresh(
                refreshToken,
                currentIp,
                currentFingerprint
            );

            log.debug("Token刷新成功, 用户ID: {}", userId);

            // 5. 计算新token过期时间
            long expiresIn = Duration.ofMinutes(15).toSeconds();

            return AuthDTO.TokenResponse.of(tokenPair.accessToken(), expiresIn);

        } catch (BusinessException e) {
            throw e;
        } catch (SecurityException e) {
            log.warn("Token刷新安全验证失败: {}", e.getMessage());
            throw new BusinessException(Code.TOKEN_INVALID);
        } catch (Exception e) {
            log.error("Token刷新处理异常", e);
            throw new BusinessException(Code.SYSTEM_ERROR);
        }
    }

    /**
     * 用户退出
     *
     * @param request HTTP请求对象
     */
    @Transactional
    public void logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                // 撤销token（添加到黑名单）
                jwtUtils.revoke(token);
                log.info("用户退出成功，token已撤销");
            }
        } catch (Exception e) {
            log.error("退出处理异常", e);
            // 退出失败不抛异常，确保用户可以正常退出
        }
    }

    /**
     * 验证当前token是否有效
     *
     * @param request HTTP请求对象
     * @return 是否有效
     */
    public boolean validateCurrentToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return false;
            }

            String currentIp = jwtUtils.extractIp(request);
            String currentFingerprint = jwtUtils.extractFingerprint(request);

            return jwtUtils.validate(token, currentIp, currentFingerprint);
        } catch (Exception e) {
            log.debug("Token验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证验证码
     */
    private void validateCaptcha(String captchaId, String captchaCode) {
        CaptchaService.Result result = captchaService.verify(captchaId, captchaCode);
        if (!result.success()) {
            log.warn("验证码验证失败: {} - {}", captchaId, result.message());
            throw new BusinessException(Code.CAPTCHA_INVALID);
        }
    }

    /**
     * 根据用户名或邮箱查找用户
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        // 判断是否为邮箱格式
        if (usernameOrEmail.contains("@")) {
            return userService.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));
        } else {
            return userService.findByUsername(usernameOrEmail)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));
        }
    }

    /**
     * 验证用户状态
     */
    private void validateUserStatus(User user) {
        switch (user.getStatus()) {
            case PENDING -> throw new BusinessException(Code.USER_NOT_ACTIVATED);
            case LOCKED -> throw new BusinessException(Code.USER_LOCKED);
            case SUSPENDED -> throw new BusinessException(Code.USER_SUSPENDED);
            case BANNED -> throw new BusinessException(Code.USER_BANNED);
            case DELETED -> throw new BusinessException(Code.USER_NOT_FOUND);
            case INACTIVE -> {
                // 非活跃用户可以登录，但可能需要额外验证
                log.info("非活跃用户登录: {}", user.getUsername());
            }
            case ACTIVE -> {
                // 正常状态，无需特殊处理
            }
        }
    }

    /**
     * 获取用户权限集合
     * TODO: 实际项目中应该从数据库查询用户角色和权限
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
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("access-token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        return null;
    }
}
