package com.example.app.model;

import java.time.Instant;

/**
 * 管理员账号实体（对应 admin_user 表）。
 */
public record AdminUser(
        Long id,
        String username,
        String passwordHash,
        String status,
        Instant lastLoginTime,
        String lastLoginIp,
        Instant createTime,
        Instant updateTime
) {

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}