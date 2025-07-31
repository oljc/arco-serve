package io.github.oljc.arcoserve.modules.auth;

import io.github.oljc.arcoserve.shared.annotation.RateLimit;
import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.Code;
import io.github.oljc.arcoserve.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证控制器 - 增强安全版本
 *
 * 支持IP地址和设备指纹双重验证的认证控制器
 *
 * 请求头说明：
 * - access-token: JWT访问令牌
 * - X-Fingerprint: 前端生成的设备指纹
 * - Authorization: 用于请求签名验证
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {

    private final CaptchaService captchaService;
    private final AuthService authService;

    /**
     * 获取验证码
     * 支持多种类型：LINE, CIRCLE, SHEAR, MATH, RANDOM, NUMBER
     * 使用限流：每个IP每分钟最多获取10次验证码
     */
    @GetMapping("/captcha")
    @RateLimit(limit = 10, window = 60)
    public ApiResponse<CaptchaService.CaptchaResponse> getCaptcha(
            @RequestParam(defaultValue = "LINE")
            @Pattern(regexp = "(?i)LINE|CIRCLE|SHEAR|MATH|RANDOM|NUMBER",
                    message = "不支持的验证码类型")
            String type,
            HttpServletRequest request) {

        var captchaType = CaptchaService.CaptchaType.valueOf(type.toUpperCase());
        var response = captchaService.create(captchaType);

        log.debug("生成验证码成功: type={}, id={}, ip={}",
                type, response.id(), getClientIp(request));

        return ApiResponse.success(response);
    }

    /**
     * 用户登录
     * 使用限流：每个IP每分钟最多尝试5次登录
     */
    @PostMapping("/login")
    @RateLimit(limit = 5, window = 60)
    public ApiResponse<AuthDTO.AuthResponse> login(
            @Valid @RequestBody AuthDTO.LoginRequest loginRequest,
            HttpServletRequest request) {

        // 验证设备指纹
        String deviceFingerprint = request.getHeader("X-Fingerprint");
        if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            log.warn("登录请求缺少设备指纹, IP: {}", getClientIp(request));
            throw new BusinessException(Code.DEVICE_FINGERPRINT_REQUIRED);
        }

        AuthDTO.AuthResponse response = authService.login(loginRequest, request);

        log.info("用户登录成功: {}, IP: {}",
                loginRequest.usernameOrEmail(), getClientIp(request));

        return ApiResponse.success(response);
    }

    /**
     * 刷新Token
     * 使用限流：每个IP每分钟最多刷新10次
     */
    @PostMapping("/refresh")
    @RateLimit(limit = 10, window = 60)
    public ApiResponse<AuthDTO.TokenResponse> refreshToken(
            @Valid @RequestBody AuthDTO.RefreshTokenRequest refreshRequest,
            HttpServletRequest request) {

        // 验证设备指纹
        String deviceFingerprint = request.getHeader("X-Fingerprint");
        if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            throw new BusinessException(Code.DEVICE_FINGERPRINT_REQUIRED);
        }

        AuthDTO.TokenResponse response = authService.refreshToken(refreshRequest, request);

        log.debug("Token刷新成功, IP: {}", getClientIp(request));

        return ApiResponse.success(response);
    }

    /**
     * 用户退出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);

        log.info("用户退出成功, IP: {}", getClientIp(request));

        return ApiResponse.success();
    }

    /**
     * 验证当前Token
     */
    @GetMapping("/validate")
    public ApiResponse<Map<String, Object>> validateToken(HttpServletRequest request) {
        boolean isValid = authService.validateCurrentToken(request);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("timestamp", LocalDateTime.now());

        if (isValid) {
            // 如果token有效，返回当前用户信息
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                response.put("username", authentication.getName());
                response.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            }
        }

        return ApiResponse.success(response);
    }

    /**
     * 获取当前用户信息
     * 需要认证
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(Code.UNAUTHORIZED);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("authorities", authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        userInfo.put("authenticated", true);
        userInfo.put("timestamp", LocalDateTime.now());

        return ApiResponse.success(userInfo);
    }

    /**
     * 验证验证码
     * 用于前端验证验证码是否正确（可选接口）
     */
    @PostMapping("/captcha/verify")
    @RateLimit(limit = 20, window = 60)
    public ApiResponse<Map<String, Object>> verifyCaptcha(
            @Valid @RequestBody AuthDTO.CaptchaVerifyRequest verifyRequest) {

        CaptchaService.Result result = captchaService.verify(verifyRequest.captchaId(), verifyRequest.captchaCode());

        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.success());
        response.put("message", result.success() ? "验证码正确" : result.message());

        return ApiResponse.success(response);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
