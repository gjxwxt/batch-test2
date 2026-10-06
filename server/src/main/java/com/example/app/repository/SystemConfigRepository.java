package com.example.app.repository;

import com.example.app.model.SystemConfig;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 系统配置仓储（内存实现）。
 *
 * <p>对应共享契约 6 表 DDL 中的 {@code system_config} 表（infra:ddl）。
 * 预置 8 项默认配置（心跳 30s / 超时倍数 3 / 归档 30 天 / 历史删除 90 天等）。</p>
 *
 * <p>同时提供两套访问 API：wp-4/wp-5 依赖的 {@code KEY_*} 常量 + {@link #findValue}/{@link #getValueOrDefault}，
 * 以及 wp-6 依赖的 {@link #findAll}/{@link #findByKey}/{@link #save}（基于 {@link SystemConfig} 模型）。</p>
 */
@Repository
public class SystemConfigRepository {

    /** 心跳间隔配置键（秒）。 */
    public static final String KEY_HEARTBEAT_INTERVAL = "heartbeat.interval";
    /** 心跳超时倍数配置键。 */
    public static final String KEY_HEARTBEAT_TIMEOUT_COUNT = "heartbeat.timeout.count";
    /** 下线实例归档天数配置键。 */
    public static final String KEY_ARCHIVE_AFTER_DAYS = "archive.after.days";
    /** 历史实例删除天数配置键。 */
    public static final String KEY_HISTORY_DELETE_AFTER_DAYS = "history.delete.after.days";

    private final ConcurrentMap<String, SystemConfig> storage = new ConcurrentHashMap<>();

    public SystemConfigRepository() {
        seedDefaults();
    }

    private void seedDefaults() {
        Instant now = Instant.now();
        put(1L, KEY_HEARTBEAT_INTERVAL, "30", "心跳间隔（秒）", now);
        put(2L, KEY_HEARTBEAT_TIMEOUT_COUNT, "3", "心跳超时倍数", now);
        put(3L, KEY_ARCHIVE_AFTER_DAYS, "30", "实例归档天数", now);
        put(4L, KEY_HISTORY_DELETE_AFTER_DAYS, "90", "历史实例删除天数", now);
        put(5L, "archive.cron", "0 22 10 * * ?", "归档定时 Cron 表达式", now);
        put(6L, "expire.check.cron", "0 0 0 * * ?", "过期检查定时 Cron 表达式", now);
        put(7L, "login.token.ttl.minutes", "120", "登录令牌 TTL（分钟）", now);
        put(8L, "elastic.quota.warning.ratio", "2", "弹性配额倍数", now);
    }

    private void put(Long id, String key, String value, String desc, Instant time) {
        storage.put(key, new SystemConfig(id, key, value, desc, time));
    }

    // ---- wp-6 API（基于 SystemConfig 模型） ----

    public List<SystemConfig> findAll() {
        List<SystemConfig> configs = new ArrayList<>(storage.values());
        configs.sort(Comparator.comparing(SystemConfig::id));
        return configs;
    }

    public Optional<SystemConfig> findByKey(String key) {
        return Optional.ofNullable(storage.get(key));
    }

    public SystemConfig save(SystemConfig config) {
        storage.put(config.configKey(), config);
        return config;
    }

    // ---- wp-4/wp-5 API（基于字符串键值） ----

    public Optional<String> findValue(String key) {
        SystemConfig config = storage.get(key);
        return config == null ? Optional.empty() : Optional.of(config.configValue());
    }

    public String getValueOrDefault(String key, String defaultValue) {
        SystemConfig config = storage.get(key);
        return config == null ? defaultValue : config.configValue();
    }

    public void put(String key, String value) {
        SystemConfig existing = storage.get(key);
        if (existing != null) {
            storage.put(key, new SystemConfig(existing.id(), key, value, existing.configDesc(), Instant.now()));
        } else {
            long nextId = storage.values().stream().mapToLong(SystemConfig::id).max().orElse(0L) + 1;
            storage.put(key, new SystemConfig(nextId, key, value, "", Instant.now()));
        }
    }

    public void clear() {
        storage.clear();
        seedDefaults();
    }
}