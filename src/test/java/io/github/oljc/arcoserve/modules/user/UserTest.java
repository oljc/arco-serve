package io.github.oljc.arcoserve.modules.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户实体类测试
 */
@DisplayName("用户实体类测试")
class UserTest {

    @Test
    @DisplayName("使用Builder创建用户")
    void testCreateUserWithBuilder() {
        // 使用Builder模式创建用户
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword123")
                .build();

        // 验证基本属性
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedPassword123", user.getPasswordHash());

        // 验证默认值
        assertEquals(User.UserStatus.PENDING, user.getStatus());
        assertEquals(User.UserType.USER, user.getUserType());
        assertFalse(user.getEmailVerified());
    }

    @Test
    @DisplayName("测试用户状态判断方法")
    void testUserStatusMethods() {
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword123")
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        // 测试状态判断
        assertTrue(user.isActive());
        assertFalse(user.isLocked());
        assertFalse(user.isDeleted());
        assertTrue(user.isEmailVerified());
    }

    @Test
    @DisplayName("测试锁定状态用户")
    void testLockedUser() {
        User user = User.builder()
                .username("lockeduser")
                .email("locked@example.com")
                .passwordHash("hashedPassword123")
                .status(User.UserStatus.LOCKED)
                .build();

        assertFalse(user.isActive());
        assertTrue(user.isLocked());
        assertFalse(user.isDeleted());
    }

    @Test
    @DisplayName("测试已删除用户")
    void testDeletedUser() {
        User user = User.builder()
                .username("deleteduser")
                .email("deleted@example.com")
                .passwordHash("hashedPassword123")
                .status(User.UserStatus.DELETED)
                .deletedAt(LocalDateTime.now())
                .build();

        assertFalse(user.isActive());
        assertFalse(user.isLocked());
        assertTrue(user.isDeleted());
    }

    @Test
    @DisplayName("测试用户类型")
    void testUserTypes() {
        User adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash("hashedPassword123")
                .userType(User.UserType.ADMIN)
                .build();

        assertEquals(User.UserType.ADMIN, adminUser.getUserType());

        User normalUser = User.builder()
                .username("user")
                .email("user@example.com")
                .passwordHash("hashedPassword123")
                .build();

        assertEquals(User.UserType.USER, normalUser.getUserType());
    }

    @Test
    @DisplayName("测试邮箱验证状态")
    void testEmailVerification() {
        User unverifiedUser = User.builder()
                .username("unverified")
                .email("unverified@example.com")
                .passwordHash("hashedPassword123")
                .build();

        assertFalse(unverifiedUser.isEmailVerified());

        User verifiedUser = User.builder()
                .username("verified")
                .email("verified@example.com")
                .passwordHash("hashedPassword123")
                .emailVerified(true)
                .build();

        assertTrue(verifiedUser.isEmailVerified());
    }
}
