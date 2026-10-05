package com.example.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员登录请求（IAS_AUTH_LOGIN）。
 */
public record LoginRequest(
        @NotBlank(message = "username must not be blank")
        String username,

        @NotBlank(message = "password must not be blank")
        String password
) {}