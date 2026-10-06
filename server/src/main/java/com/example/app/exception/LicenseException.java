package com.example.app.exception;

/**
 * 授权/实例业务异常（wp-3 授权管理 / wp-4 客户端交互 / wp-5 实例生命周期 / wp-8 统计）。
 *
 * <p>携带 {@link ErrorCode}，由 {@link GlobalExceptionHandler} 统一转换为
 * {@link ApiErrorResponse} 并映射正确 HTTP 状态码。</p>
 */
public class LicenseException extends RuntimeException {

    private final ErrorCode errorCode;

    public LicenseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LicenseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}