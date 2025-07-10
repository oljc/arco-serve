package io.github.oljc.arcoserve.shared.exception;

/**
 * 统一状态码
 */
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    VALIDATION_ERROR(422, "参数验证失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_DISABLED(1003, "用户已禁用"),
    USER_LOCKED(1004, "用户已锁定"),

    INVALID_CREDENTIALS(1101, "用户名或密码错误"),
    TOKEN_EXPIRED(1102, "令牌已过期"),
    TOKEN_INVALID(1103, "令牌无效"),
    ACCOUNT_LOCKED(1104, "账户已锁定"),

    PERMISSION_DENIED(2001, "权限不足"),
    ROLE_NOT_FOUND(2002, "角色不存在"),
    ROLE_ALREADY_EXISTS(2003, "角色已存在"),

    SIGNATURE_MISSING(3001, "签名缺失"),
    SIGNATURE_INVALID(3002, "签名无效"),
    SIGNATURE_REPLAY(3003, "检测到重放攻击"),
    SIGNATURE_EXPIRED(3004, "签名已过期"),
    SIGNATURE_VERIFICATION_FAILED(3005, "签名验证失败"),

    DATA_NOT_FOUND(4001, "数据不存在"),
    DATA_ALREADY_EXISTS(4002, "数据已存在"),
    DATA_INTEGRITY_VIOLATION(4003, "数据完整性约束违反"),

    FILE_NOT_FOUND(5001, "文件不存在"),
    FILE_UPLOAD_FAILED(5002, "文件上传失败"),
    FILE_SIZE_EXCEEDED(5003, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(5004, "文件类型不支持"),
    ;

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
