package com.example.app.model;

import java.time.Instant;

/**
 * 系统配置项。
 * 对应 schema 中 system_config 表（8 项默认配置）。
 */
public record SystemConfig(
        Long id,
        String configKey,
        String configValue,
        String configDesc,
        Instant updateTime
) {}