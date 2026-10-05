package com.example.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 修改密码请求（IAS_AUTH_CHANGE_PWD）。
 */
public record ChangePasswordRequest(
        @NotBlank(message = "oldPassword must not be blank")
        String oldPassword,

        @NotBlank(message = "newPassword must not be blank")
        String newPassword
) {}