package io.github.oljc.arcoserve.modules.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 根据用户名查找用户（不包含已删除的用户）
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsernameAndNotDeleted(@Param("username") String username);

    /**
     * 根据邮箱查找用户（不包含已删除的用户）
     *
     * @param email 邮箱地址
     * @return 用户信息
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    /**
     * 根据用户ID查找用户（不包含已删除的用户）
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.deletedAt IS NULL")
    Optional<User> findByUserIdAndNotDeleted(@Param("userId") Long userId);

    /**
     * 检查用户名是否存在（不包含已删除的用户）
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsernameAndNotDeleted(@Param("username") String username);

    /**
     * 检查邮箱是否存在（不包含已删除的用户）
     *
     * @param email 邮箱地址
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    /**
     * 根据用户状态分页查询用户（不包含已删除的用户）
     *
     * @param status   用户状态
     * @param pageable 分页参数
     * @return 分页用户列表
     */
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt IS NULL")
    Page<User> findByStatusAndNotDeleted(@Param("status") User.UserStatus status, Pageable pageable);

    /**
     * 根据用户类型分页查询用户（不包含已删除的用户）
     *
     * @param userType 用户类型
     * @param pageable 分页参数
     * @return 分页用户列表
     */
    @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.deletedAt IS NULL")
    Page<User> findByUserTypeAndNotDeleted(@Param("userType") User.UserType userType, Pageable pageable);

    /**
     * 查询所有未删除的用户
     *
     * @param pageable 分页参数
     * @return 分页用户列表
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAllNotDeleted(Pageable pageable);

    /**
     * 根据邮箱验证状态分页查询用户（不包含已删除的用户）
     *
     * @param emailVerified 邮箱验证状态
     * @param pageable      分页参数
     * @return 分页用户列表
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = :emailVerified AND u.deletedAt IS NULL")
    Page<User> findByEmailVerifiedAndNotDeleted(@Param("emailVerified") Boolean emailVerified, Pageable pageable);
}
