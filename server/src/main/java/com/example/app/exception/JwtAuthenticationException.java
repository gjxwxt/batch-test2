package com.example.app.exception;

/**
 * JWT 认证失败异常（令牌缺失/无效/过期）。映射 AUTH_002（HTTP 401）。
 */
public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}