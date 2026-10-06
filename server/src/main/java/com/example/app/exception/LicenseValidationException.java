package com.example.app.exception;

/**
 * 授权文件校验失败异常（服务端启动自检 / 授权导入 / 防篡改）。
 * 由 GlobalExceptionHandler 统一转换为 ApiErrorResponse。
 */
public class LicenseValidationException extends RuntimeException {

    public LicenseValidationException(String message) {
        super(message);
    }

    public LicenseValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}