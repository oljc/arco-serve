package io.github.oljc.arcoserve.modules.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户相关的所有DTO类 - 使用记录类优化性能
 */
public final class UserDTO {

    /**
     * 用户创建请求
     */
    public record CreateRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 32, message = "用户名长度必须在3-32位之间")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
            String username,

            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email,

            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 128, message = "密码长度必须在8-128位之间")
            String password,

            User.UserType userType
    ) {
        public CreateRequest {
            // 使用紧凑构造器进行验证和默认值设置
            if (userType == null) {
                userType = User.UserType.USER;
            }
        }
    }

    /**
     * 用户更新请求
     */
    public record UpdateRequest(
            @Size(min = 3, max = 32, message = "用户名长度必须在3-32位之间")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
            String username,

            @Email(message = "邮箱格式不正确")
            String email,

            User.UserStatus status,

            User.UserType userType,

            Boolean emailVerified
    ) {
        /**
         * 检查是否有任何字段需要更新
         *
         * @return 是否有更新内容
         */
        public boolean hasUpdates() {
            return username != null || email != null || status != null ||
                   userType != null || emailVerified != null;
        }
    }

    /**
     * 密码更新请求
     */
    public record PasswordUpdateRequest(
            @NotBlank(message = "新密码不能为空")
            @Size(min = 8, max = 128, message = "新密码长度必须在8-128位之间")
            String newPassword,

            String oldPassword
    ) {
        /**
         * 创建简单的密码更新请求（不需要验证旧密码）
         *
         * @param newPassword 新密码
         * @return 密码更新请求
         */
        public static PasswordUpdateRequest of(String newPassword) {
            return new PasswordUpdateRequest(newPassword, null);
        }
    }

    /**
     * 用户响应 - 高性能序列化
     */
    public record Response(
            UUID id,
            Long userId,
            String username,
            String email,
            User.UserStatus status,
            User.UserType userType,
            Boolean emailVerified,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        /**
         * 从User实体创建UserResponse - 高性能静态工厂方法
         *
         * @param user 用户实体
         * @return 用户响应DTO
         */
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getStatus(),
                    user.getUserType(),
                    user.getEmailVerified(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }

    // 私有构造函数，防止实例化
    private UserDTO() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
