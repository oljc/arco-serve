package io.github.oljc.arcoserve.shared.exception;

import io.github.oljc.arcoserve.shared.exception.Code;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(Code code) {
        super(code.getMessage());
        this.code = code.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(Code code, Throwable cause) {
        super(code.getMessage(), cause);
        this.code = code.getCode();
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BusinessException(Code code, String message) {
        super(message);
        this.code = code.getCode();
    }

    public int getCode() {
        return code;
    }
}
