package io.github.oljc.arcoserve.modules.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户实体类
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    @Builder.Default
    private UserType userType = UserType.USER;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        PENDING,     // 待激活
        ACTIVE,      // 正常使用
        INACTIVE,    // 长期未登录
        LOCKED,      // 登录被锁定
        SUSPENDED,   // 风控暂停
        BANNED,      // 封禁
        DELETED      // 已删除
    }

    /**
     * 用户类型枚举
     */
    public enum UserType {
        SUPER_ADMIN,  // 超级管理员
        ADMIN,        // 管理员
        TEST,         // 测试账号
        OPERATOR,     // 运营审核人员
        API,          // API系统集成
        USER,         // 普通用户
        GUEST,        // 游客、未注册用户
        BOT           // 系统使用账号
    }

    /**
     * 判断用户是否已激活
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * 判断用户是否被锁定
     */
    public boolean isLocked() {
        return status == UserStatus.LOCKED;
    }

    /**
     * 判断用户是否已删除
     */
    public boolean isDeleted() {
        return deletedAt != null || status == UserStatus.DELETED;
    }

    /**
     * 判断邮箱是否已验证
     */
    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }
}
