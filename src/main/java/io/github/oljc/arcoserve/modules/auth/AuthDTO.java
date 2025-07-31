package io.github.oljc.arcoserve.modules.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.oljc.arcoserve.modules.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证相关的所有DTO类
 */
public final class AuthDTO {

    /**
     * 用户注册请求
     */
    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 32, message = "用户名长度必须在3-32位之间")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
            String username,

            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email,

            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 128, message = "密码长度必须在8-128位之间")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                    message = "密码必须包含大小写字母、数字和特殊字符")
            String password,

            @NotBlank(message = "确认密码不能为空")
            String confirmPassword,

            @NotBlank(message = "验证码ID不能为空")
            String captchaId,

            @NotBlank(message = "验证码不能为空")
            @Size(min = 1, max = 10, message = "验证码长度必须在1-10位之间")
            String captchaCode
    ) {
        /**
         * 验证两次密码是否一致
         */
        public boolean isPasswordMatched() {
            return password != null && password.equals(confirmPassword);
        }
    }

    /**
     * 用户登录请求
     */
    public record LoginRequest(
            @NotBlank(message = "用户名或邮箱不能为空")
            String usernameOrEmail,

            @NotBlank(message = "密码不能为空")
            String password,

            @NotBlank(message = "验证码ID不能为空")
            String captchaId,

            @NotBlank(message = "验证码不能为空")
            @Size(min = 1, max = 10, message = "验证码长度必须在1-10位之间")
            String captchaCode,

            Boolean rememberMe
    ) {
        public LoginRequest {
            if (rememberMe == null) {
                rememberMe = false;
            }
        }
    }

    /**
     * 验证码请求
     */
    public record VerificationCodeRequest(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email,

            @NotBlank(message = "验证码类型不能为空")
            @Pattern(regexp = "^(REGISTER|LOGIN|FORGOT_PASSWORD|RESET_PASSWORD)$",
                    message = "验证码类型必须是REGISTER、LOGIN、FORGOT_PASSWORD或RESET_PASSWORD")
            String codeType
    ) {}

    /**
     * 验证码响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VerificationCodeResponse(
            String message,
            String email,
            String codeType,
            Long expiresIn
    ) {
        public static VerificationCodeResponse of(String message, String email, String codeType, Long expiresIn) {
            return new VerificationCodeResponse(message, email, codeType, expiresIn);
        }
    }

    /**
     * 刷新token请求
     */
    public record RefreshTokenRequest(
            @NotBlank(message = "刷新token不能为空")
            String refreshToken
    ) {}

    /**
     * 忘记密码请求
     */
    public record ForgotPasswordRequest(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email
    ) {}

    /**
     * 重置密码请求
     */
    public record ResetPasswordRequest(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email,

            @NotBlank(message = "新密码不能为空")
            @Size(min = 8, max = 128, message = "密码长度必须在8-128位之间")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                    message = "密码必须包含大小写字母、数字和特殊字符")
            String newPassword,

            @NotBlank(message = "确认密码不能为空")
            String confirmPassword
    ) {
        /**
         * 验证两次密码是否一致
         */
        public boolean isPasswordMatched() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }
    }

    /**
     * 认证响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            UserInfo userInfo
    ) {
        public AuthResponse {
            if (tokenType == null) {
                tokenType = "Bearer";
            }
        }

        public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, User user) {
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    expiresIn,
                    UserInfo.from(user)
            );
        }
    }

    /**
     * 用户信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserInfo(
            UUID id,
            Long userId,
            String username,
            String email,
            String status,
            String userType,
            Boolean emailVerified,
            LocalDateTime createdAt
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getStatus().name(),
                    user.getUserType().name(),
                    user.getEmailVerified(),
                    user.getCreatedAt()
            );
        }
    }

    /**
     * 令牌响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TokenResponse(
            String accessToken,
            String tokenType,
            Long expiresIn
    ) {
        public TokenResponse {
            if (tokenType == null) {
                tokenType = "Bearer";
            }
        }

        public static TokenResponse of(String accessToken, Long expiresIn) {
            return new TokenResponse(accessToken, "Bearer", expiresIn);
        }
    }

    /**
     * 图形验证码验证请求
     */
    public record CaptchaVerifyRequest(
            @NotBlank(message = "验证码ID不能为空")
            String captchaId,

            @NotBlank(message = "验证码不能为空")
            @Size(min = 4, max = 6, message = "验证码长度必须在4-6位之间")
            String captchaCode
    ) {}

    /**
     * 图形验证码响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CaptchaResponse(
            String captchaId,
            String captchaImage,
            Long expiresIn
    ) {
        public static CaptchaResponse of(String captchaId, String captchaImage, Long expiresIn) {
            return new CaptchaResponse(captchaId, captchaImage, expiresIn);
        }
    }

    /**
     * Token刷新响应 - 最佳实践版本
     * 包含新的access token和refresh token
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TokenRefreshResponse(
            @NotBlank
            String accessToken,

            @NotBlank
            String refreshToken,

            Long expiresIn
    ) {
        public static TokenRefreshResponse of(String accessToken, String refreshToken, Long expiresIn) {
            return new TokenRefreshResponse(accessToken, refreshToken, expiresIn);
        }
    }

    /**
     * 登出请求
     */
    public record LogoutRequest(
            String refreshToken
    ) {
        // refresh token可以为空，因为可能只有access token
    }

    /**
     * 用户权限响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserPermissionsResponse(
            UUID userId,
            String username,
            String userType,
            java.util.Set<String> permissions,
            LocalDateTime lastLogin
    ) {
        public static UserPermissionsResponse of(User user, java.util.Set<String> permissions) {
            return new UserPermissionsResponse(
                user.getId(),
                user.getUsername(),
                user.getUserType().name(),
                permissions,
                user.getLastLoginAt()
            );
        }
    }

    /**
     * 安全事件记录
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SecurityEventResponse(
            String eventType,
            UUID userId,
            String ipAddress,
            String userAgent,
            LocalDateTime timestamp,
            String details
    ) {
        public static SecurityEventResponse of(String eventType, UUID userId, String ipAddress,
                                             String userAgent, String details) {
            return new SecurityEventResponse(eventType, userId, ipAddress, userAgent,
                                           LocalDateTime.now(), details);
        }
    }

    /**
     * Token信息响应（用于调试和监控）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TokenInfoResponse(
            String tokenType,
            String subject,
            String issuer,
            String audience,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            LocalDateTime notBefore,
            java.util.Set<String> permissions,
            String deviceFingerprint
    ) {
        public static TokenInfoResponse fromJwtClaims(io.jsonwebtoken.Claims claims, java.util.Set<String> permissions) {
            return new TokenInfoResponse(
                claims.get("tokenType", String.class),
                claims.getSubject(),
                claims.getIssuer(),
                claims.getAudience().iterator().next(),
                claims.getIssuedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                claims.getExpiration().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                claims.getNotBefore() != null ?
                    claims.getNotBefore().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                permissions,
                claims.get("deviceFingerprint", String.class)
            );
        }
    }

    /**
     * 密码强度检查响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PasswordStrengthResponse(
            int score,
            String level,
            java.util.List<String> suggestions,
            boolean isValid
    ) {
        public static PasswordStrengthResponse of(int score, String level, java.util.List<String> suggestions) {
            return new PasswordStrengthResponse(score, level, suggestions, score >= 3);
        }
    }
}
