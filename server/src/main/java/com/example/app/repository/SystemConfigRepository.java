package com.example.app.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 系统配置仓储（内存实现）。
 *
 * <p>对应共享契约 6 表 DDL 中的 {@code system_config} 表（infra:ddl）。
 * 预置 8 项默认配置（心跳 30s / 超时倍数 3 等）。</p>
 */
@Repository
public class SystemConfigRepository {

    /** 心跳间隔配置键（秒）。 */
    public static final String KEY_HEARTBEAT_INTERVAL = "heartbeat.interval";
    /** 心跳超时倍数配置键。 */
    public static final String KEY_HEARTBEAT_TIMEOUT_COUNT = "heartbeat.timeout.count";

    private final ConcurrentMap<String, String> storage = new ConcurrentHashMap<>();

    public SystemConfigRepository() {
        // 预置默认配置（与 V1__init_schema.sql 种子一致）
        storage.put(KEY_HEARTBEAT_INTERVAL, "30");
        storage.put(KEY_HEARTBEAT_TIMEOUT_COUNT, "3");
        storage.put("archive.after.days", "30");
        storage.put("history.delete.after.days", "90");
        storage.put("archive.cron", "0 22 10 * * ?");
        storage.put("expire.check.cron", "0 0 0 * * ?");
        storage.put("login.token.ttl.minutes", "120");
        storage.put("elastic.quota.warning.ratio", "2");
    }

    public Optional<String> findValue(String key) {
        return Optional.ofNullable(storage.get(key));
    }

    public String getValueOrDefault(String key, String defaultValue) {
        return storage.getOrDefault(key, defaultValue);
    }

    public void put(String key, String value) {
        storage.put(key, value);
    }

    public void clear() {
        storage.clear();
    }
}