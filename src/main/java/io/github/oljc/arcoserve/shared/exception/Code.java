package io.github.oljc.arcoserve.shared.exception;

/**
 * 统一状态码
 */
public enum Code {
    SUCCESS(200, "操作成功"),
    ERROR(500, "系统内部错误"),
    SYSTEM_ERROR(500, "系统内部错误"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    VALIDATION_ERROR(422, "参数错误，请检查后再试"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    SIGN_MISSING(3001, "签名缺失"),
    SIGN_INVALID(3002, "签名无效"),
    SIGN_REPLAY(3003, "检测到重放攻击"),
    SIGN_EXPIRED(3004, "签名已过期"),
    SIGN_ERROR(3005, "签名验证失败"),

    FILE_NOT_FOUND(5001, "文件不存在"),
    FILE_UPLOAD_FAILED(5002, "文件上传失败"),
    FILE_SIZE_EXCEEDED(5003, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(5004, "文件类型不支持"),

    DATA_NOT_FOUND(10001, "数据不存在"),
    DATA_EXISTS(10002, "数据已存在"),
    DATA_VIOLATION(10003, "数据异常"),

    // 用户相关错误码
    USER_NOT_FOUND(11001, "用户不存在"),
    USER_NAME_EXISTS(11002, "用户名已存在"),
    USER_EMAIL_EXISTS(11003, "邮箱已存在"),
    USER_PASSWORD_INVALID(11004, "密码错误"),
    USER_LOCKED(11005, "账户已锁定"),
    USER_DISABLED(11006, "账户已禁用"),
    USER_NOT_VERIFIED(11007, "邮箱未验证"),
    USER_NOT_ALLOWED(11008, "操作不允许"),
    USER_NOT_ACTIVATED(11009, "账户未激活"),
    USER_SUSPENDED(11010, "账户已暂停"),
    USER_BANNED(11011, "账户已封禁"),

    // 认证相关错误码
    TOKEN_INVALID(11101, "登录已失效"),
    TOKEN_EXPIRED(11102, "登录已过期"),
    TOKEN_MISSING(11103, "请先登录"),
    LOGIN_FAILED(11104, "登录失败"),
    REGISTRATION_FAILED(11105, "注册失败"),
    DEVICE_FINGERPRINT_REQUIRED(11106, "设备指纹验证失败"),
    CAPTCHA_INVALID(11107, "验证码错误"),

    PARAM_INVALID(12001, "参数错误");

    private final int code;
    private final String message;

    Code(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static Code fromCode(int code) {
        for (Code c : values()) {
            if (c.code == code) {
                return c;
            }
        }
        return ERROR;
    }
}
