package io.github.oljc.arcoserve.modules.user;

import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.Code;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * 用户服务类 - 直接实现，不使用接口
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    /**
     * 创建新用户
     *
     * @param createRequest 用户创建请求
     * @return 创建的用户信息
     */
    public User createUser(UserDTO.CreateRequest createRequest) {
        log.info("Creating user with username: {}", createRequest.username());

        // 验证用户名和邮箱是否已存在
        if (userRepository.existsByUsernameAndNotDeleted(createRequest.username())) {
            throw new BusinessException(Code.USER_NAME_EXISTS);
        }

        if (userRepository.existsByEmailAndNotDeleted(createRequest.email())) {
            throw new BusinessException(Code.USER_EMAIL_EXISTS);
        }

        // 生成唯一的用户ID
        Long userId = generateUniqueUserId();

        // 创建用户
        User user = User.builder()
                .userId(userId)
                .username(createRequest.username())
                .email(createRequest.email())
                .passwordHash(passwordEncoder.encode(createRequest.password()))
                .userType(createRequest.userType())
                .status(User.UserStatus.PENDING)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id)
                .filter(user -> !user.isDeleted());
    }

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameAndNotDeleted(username);
    }

    /**
     * 根据邮箱查找用户
     *
     * @param email 邮箱地址
     * @return 用户信息
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndNotDeleted(email);
    }

    /**
     * 根据用户ID查找用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUserId(Long userId) {
        return userRepository.findByUserIdAndNotDeleted(userId);
    }

    /**
     * 更新用户信息
     *
     * @param id 用户ID
     * @param updateRequest 更新请求
     * @return 更新后的用户信息
     */
    public User updateUser(UUID id, UserDTO.UpdateRequest updateRequest) {
        log.info("Updating user with ID: {}", id);

        if (!updateRequest.hasUpdates()) {
            throw new BusinessException(Code.PARAM_INVALID);
        }

        User user = findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        // 更新用户名
        if (updateRequest.username() != null && !updateRequest.username().equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndNotDeleted(updateRequest.username())) {
                throw new BusinessException(Code.USER_NAME_EXISTS);
            }
            user.setUsername(updateRequest.username());
        }

        // 更新邮箱
        if (updateRequest.email() != null && !updateRequest.email().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndNotDeleted(updateRequest.email())) {
                throw new BusinessException(Code.USER_EMAIL_EXISTS);
            }
            user.setEmail(updateRequest.email());
            user.setEmailVerified(false); // 邮箱变更后需要重新验证
        }

        // 更新状态
        if (updateRequest.status() != null) {
            user.setStatus(updateRequest.status());
        }

        // 更新用户类型
        if (updateRequest.userType() != null) {
            user.setUserType(updateRequest.userType());
        }

        // 更新邮箱验证状态
        if (updateRequest.emailVerified() != null) {
            user.setEmailVerified(updateRequest.emailVerified());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", updatedUser.getId());

        return updatedUser;
    }

    /**
     * 更新用户密码
     *
     * @param id 用户ID
     * @param newPassword 新密码
     * @return 更新后的用户信息
     */
    public User updatePassword(UUID id, String newPassword) {
        log.info("Updating password for user ID: {}", id);

        User user = findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);

        log.info("Password updated successfully for user ID: {}", updatedUser.getId());
        return updatedUser;
    }

    /**
     * 激活用户
     *
     * @param id 用户ID
     * @return 更新后的用户信息
     */
    public User activateUser(UUID id) {
        log.info("Activating user with ID: {}", id);

        User user = findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        user.setStatus(User.UserStatus.ACTIVE);
        User activatedUser = userRepository.save(user);

        log.info("User activated successfully with ID: {}", activatedUser.getId());
        return activatedUser;
    }

    /**
     * 锁定用户
     *
     * @param id 用户ID
     * @param reason 锁定原因
     * @return 更新后的用户信息
     */
    public User lockUser(UUID id, String reason) {
        log.info("Locking user with ID: {}, reason: {}", id, reason);

        User user = findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        user.setStatus(User.UserStatus.LOCKED);
        User lockedUser = userRepository.save(user);

        log.info("User locked successfully with ID: {}", lockedUser.getId());
        return lockedUser;
    }

    /**
     * 解锁用户
     *
     * @param id 用户ID
     * @return 更新后的用户信息
     */
    public User unlockUser(UUID id) {
        log.info("Unlocking user with ID: {}", id);

        User user = findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        user.setStatus(User.UserStatus.ACTIVE);
        User unlockedUser = userRepository.save(user);

        log.info("User unlocked successfully with ID: {}", unlockedUser.getId());
        return unlockedUser;
    }

    /**
     * 软删除用户
     *
     * @param id 用户ID
     */
    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);

        User user = findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);

        log.info("User deleted successfully with ID: {}", id);
    }

    /**
     * 验证邮箱
     *
     * @param id 用户ID
     * @return 更新后的用户信息
     */
    public User verifyEmail(UUID id) {
        log.info("Verifying email for user with ID: {}", id);

        User user = findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        user.setEmailVerified(true);
        User verifiedUser = userRepository.save(user);

        log.info("Email verified successfully for user ID: {}", verifiedUser.getId());
        return verifiedUser;
    }

    /**
     * 分页查询用户
     *
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    @Transactional(readOnly = true)
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAllNotDeleted(pageable);
    }

    /**
     * 根据状态分页查询用户
     *
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    @Transactional(readOnly = true)
    public Page<User> findUsersByStatus(User.UserStatus status, Pageable pageable) {
        return userRepository.findByStatusAndNotDeleted(status, pageable);
    }

    /**
     * 根据类型分页查询用户
     *
     * @param userType 用户类型
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    @Transactional(readOnly = true)
    public Page<User> findUsersByType(User.UserType userType, Pageable pageable) {
        return userRepository.findByUserTypeAndNotDeleted(userType, pageable);
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameAndNotDeleted(username);
    }

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndNotDeleted(email);
    }

    /**
     * 验证用户密码
     *
     * @param user 用户信息
     * @param rawPassword 原始密码
     * @return 是否匹配
     */
    @Transactional(readOnly = true)
    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    /**
     * 生成唯一的用户ID
     *
     * @return 唯一的用户ID
     */
    private Long generateUniqueUserId() {
        Long userId;
        do {
            userId = 1000000000L + random.nextLong(9000000000L); // 生成10位数字
        } while (userRepository.findByUserIdAndNotDeleted(userId).isPresent());

        return userId;
    }
}
