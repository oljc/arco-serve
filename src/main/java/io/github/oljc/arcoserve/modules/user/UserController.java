package io.github.oljc.arcoserve.modules.user;

import io.github.oljc.arcoserve.shared.annotation.Signature;
import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.Code;
import io.github.oljc.arcoserve.shared.response.ApiResponse;
import io.github.oljc.arcoserve.shared.response.PageData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Signature
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     *
     * @param createRequest 用户创建请求
     * @return 创建的用户信息
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDTO.Response> createUser(@Valid @RequestBody UserDTO.CreateRequest createRequest) {
        log.info("Creating user with username: {}", createRequest.username());

        User user = userService.createUser(createRequest);
        UserDTO.Response response = UserDTO.Response.from(user);

        return ApiResponse.success(response, "用户创建成功");
    }

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<UserDTO.Response> getUserById(@PathVariable UUID id) {
        log.info("Getting user by ID: {}", id);

        User user = userService.findById(id)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        UserDTO.Response response = UserDTO.Response.from(user);
        return ApiResponse.success(response);
    }

    /**
     * 根据用户名获取用户详情
     *
     * @param username 用户名
     * @return 用户详情
     */
    @GetMapping("/username/{username}")
    public ApiResponse<UserDTO.Response> getUserByUsername(@PathVariable String username) {
        log.info("Getting user by username: {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        UserDTO.Response response = UserDTO.Response.from(user);
        return ApiResponse.success(response);
    }

    /**
     * 根据用户ID获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @GetMapping("/user-id/{userId}")
    public ApiResponse<UserDTO.Response> getUserByUserId(@PathVariable Long userId) {
        log.info("Getting user by user ID: {}", userId);

        User user = userService.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(Code.USER_NOT_FOUND));

        UserDTO.Response response = UserDTO.Response.from(user);
        return ApiResponse.success(response);
    }

    /**
     * 更新用户信息
     *
     * @param id            用户ID
     * @param updateRequest 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}")
    public ApiResponse<UserDTO.Response> updateUser(@PathVariable UUID id,
                                               @Valid @RequestBody UserDTO.UpdateRequest updateRequest) {
        log.info("Updating user with ID: {}", id);

        User user = userService.updateUser(id, updateRequest);
        UserDTO.Response response = UserDTO.Response.from(user);

        return ApiResponse.success(response, "用户信息更新成功");
    }

    /**
     * 更新用户密码
     *
     * @param id                   用户ID
     * @param passwordUpdateRequest 密码更新请求
     * @return 更新结果
     */
    @PutMapping("/{id}/password")
    public ApiResponse<Void> updatePassword(@PathVariable UUID id,
                                          @Valid @RequestBody UserDTO.PasswordUpdateRequest passwordUpdateRequest) {
        log.info("Updating password for user ID: {}", id);

        userService.updatePassword(id, passwordUpdateRequest.newPassword());
        return ApiResponse.success(null, "密码更新成功");
    }

    /**
     * 激活用户
     *
     * @param id 用户ID
     * @return 激活后的用户信息
     */
    @PostMapping("/{id}/activate")
    public ApiResponse<UserDTO.Response> activateUser(@PathVariable UUID id) {
        log.info("Activating user with ID: {}", id);

        User user = userService.activateUser(id);
        UserDTO.Response response = UserDTO.Response.from(user);

        return ApiResponse.success(response, "用户激活成功");
    }

    /**
     * 锁定用户
     *
     * @param id     用户ID
     * @param reason 锁定原因
     * @return 锁定后的用户信息
     */
    @PostMapping("/{id}/lock")
    public ApiResponse<UserDTO.Response> lockUser(@PathVariable UUID id,
                                            @RequestParam(required = false) String reason) {
        log.info("Locking user with ID: {}, reason: {}", id, reason);

        User user = userService.lockUser(id, reason);
        UserDTO.Response response = UserDTO.Response.from(user);

        return ApiResponse.success(response, "用户锁定成功");
    }

    /**
     * 解锁用户
     *
     * @param id 用户ID
     * @return 解锁后的用户信息
     */
    @PostMapping("/{id}/unlock")
    public ApiResponse<UserDTO.Response> unlockUser(@PathVariable UUID id) {
        log.info("Unlocking user with ID: {}", id);

        User user = userService.unlockUser(id);
        UserDTO.Response response = UserDTO.Response.from(user);

        return ApiResponse.success(response, "用户解锁成功");
    }

    /**
     * 删除用户（软删除）
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID id) {
        log.info("Deleting user with ID: {}", id);

        userService.deleteUser(id);
        return ApiResponse.success(null, "用户删除成功");
    }

    /**
     * 验证用户邮箱
     *
     * @param id 用户ID
     * @return 验证后的用户信息
     */
    @PostMapping("/{id}/verify-email")
    public ApiResponse<UserDTO.Response> verifyEmail(@PathVariable UUID id) {
        log.info("Verifying email for user with ID: {}", id);

        User user = userService.verifyEmail(id);
        UserDTO.Response response = UserDTO.Response.from(user);

        return ApiResponse.success(response, "邮箱验证成功");
    }

    /**
     * 分页查询所有用户
     *
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param sort 排序字段
     * @param direction 排序方向
     * @return 用户分页数据
     */
    @GetMapping
    public ApiResponse<PageData<UserDTO.Response>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("Getting all users, page: {}, size: {}, sort: {}, direction: {}",
                page, size, sort, direction);

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sort));
        Page<User> userPage = userService.findAllUsers(pageable);

        Page<UserDTO.Response> responsePage = userPage.map(UserDTO.Response::from);
        PageData<UserDTO.Response> pageData = PageData.of(responsePage);

        return ApiResponse.success(pageData);
    }

    /**
     * 根据状态分页查询用户
     *
     * @param status 用户状态
     * @param page   页码
     * @param size   每页大小
     * @param sort   排序字段
     * @param direction 排序方向
     * @return 用户分页数据
     */
    @GetMapping("/status/{status}")
    public ApiResponse<PageData<UserDTO.Response>> getUsersByStatus(
            @PathVariable User.UserStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("Getting users by status: {}, page: {}, size: {}", status, page, size);

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sort));
        Page<User> userPage = userService.findUsersByStatus(status, pageable);

        Page<UserDTO.Response> responsePage = userPage.map(UserDTO.Response::from);
        PageData<UserDTO.Response> pageData = PageData.of(responsePage);

        return ApiResponse.success(pageData);
    }

    /**
     * 根据类型分页查询用户
     *
     * @param userType 用户类型
     * @param page     页码
     * @param size     每页大小
     * @param sort     排序字段
     * @param direction 排序方向
     * @return 用户分页数据
     */
    @GetMapping("/type/{userType}")
    public ApiResponse<PageData<UserDTO.Response>> getUsersByType(
            @PathVariable User.UserType userType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("Getting users by type: {}, page: {}, size: {}", userType, page, size);

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sort));
        Page<User> userPage = userService.findUsersByType(userType, pageable);

        Page<UserDTO.Response> responsePage = userPage.map(UserDTO.Response::from);
        PageData<UserDTO.Response> pageData = PageData.of(responsePage);

        return ApiResponse.success(pageData);
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    @GetMapping("/check/username/{username}")
    @Signature(required = false)
    public ApiResponse<Boolean> checkUsernameExists(@PathVariable String username) {
        log.info("Checking if username exists: {}", username);

        boolean exists = userService.existsByUsername(username);
        return ApiResponse.success(exists);
    }

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     * @return 是否存在
     */
    @GetMapping("/check/email/{email}")
    @Signature(required = false)
    public ApiResponse<Boolean> checkEmailExists(@PathVariable String email) {
        log.info("Checking if email exists: {}", email);

        boolean exists = userService.existsByEmail(email);
        return ApiResponse.success(exists);
    }
}
