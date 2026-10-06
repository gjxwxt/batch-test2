package com.example.app.repository;

import com.example.app.model.AdminUser;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 管理员账号仓储（内存实现，种子账号 admin/Admin@123456）。
 * 后续可替换为 MyBatis-Plus + PostgreSQL 实现。
 */
@Repository
public class AdminUserRepository {

    private final ConcurrentMap<String, AdminUser> storage = new ConcurrentHashMap<>();

    public AdminUserRepository() {
        // 种子账号（O4 裁决）：admin / Admin@123456（BCrypt 哈希），status=ACTIVE
        Instant now = Instant.now();
        AdminUser seed = new AdminUser(
                1L,
                "admin",
                "$2a$10$rbkL4Q3ePh.z5w2SGTkvJuQ0P6l5ui7D7fqNxD5RWuVfdW5oEQE/m",
                "ACTIVE",
                null,
                null,
                now,
                now
        );
        storage.put(seed.username(), seed);
    }

    public Optional<AdminUser> findByUsername(String username) {
        return Optional.ofNullable(storage.get(username));
    }

    public AdminUser save(AdminUser user) {
        storage.put(user.username(), user);
        return user;
    }
}