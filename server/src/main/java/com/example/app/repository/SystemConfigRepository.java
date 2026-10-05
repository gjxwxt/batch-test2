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
 * 初始化 8 项默认配置：心跳 30s/超时倍数 3/归档 30 天/历史删除 90 天/Cron/令牌 TTL/弹性配额倍数 2。
 */
@Repository
public class SystemConfigRepository {

    private final ConcurrentMap<String, SystemConfig> storage = new ConcurrentHashMap<>();

    public SystemConfigRepository() {
        seedDefaults();
    }

    private void seedDefaults() {
        Instant now = Instant.now();
        put(1L, "heartbeat.interval", "30", "心跳间隔（秒）", now);
        put(2L, "heartbeat.timeout_multiplier", "3", "心跳超时倍数", now);
        put(3L, "instance.archive_days", "30", "实例归档天数", now);
        put(4L, "history.delete_days", "90", "历史实例删除天数", now);
        put(5L, "scheduler.cron", "0 0 2 * * ?", "定时任务 Cron 表达式", now);
        put(6L, "token.ttl_seconds", "7200", "令牌 TTL（秒）", now);
        put(7L, "elastic.quota_multiplier", "2", "弹性配额倍数", now);
        put(8L, "system.heartbeat_enabled", "true", "心跳检测开关", now);
    }

    private void put(Long id, String key, String value, String desc, Instant time) {
        storage.put(key, new SystemConfig(id, key, value, desc, time));
    }

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

    public void clear() {
        storage.clear();
        seedDefaults();
    }
}