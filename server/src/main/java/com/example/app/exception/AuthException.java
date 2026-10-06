package com.example.app.exception;

/**
 * 认证/用户业务异常，携带统一错误码。
 * 由 GlobalExceptionHandler 转换为 ApiErrorResponse。
 */
public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;

    public AuthException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}