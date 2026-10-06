package com.example.app.model;

/**
 * 管理员登录响应（IAS_AUTH_LOGIN）。
 */
public record LoginResponse(
        String token,
        String username,
        String tokenType
) {}