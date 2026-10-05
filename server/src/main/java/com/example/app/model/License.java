package com.example.app.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 授权（license）领域模型。
 *
 * <p>对应共享契约 6 表 DDL 中的 {@code license} 表（infra:ddl）。
 * 配额计数（used_instances / remaining_instances / used_cpus / used_memory）以数据库为准（req-28 强一致）。</p>
 *
 * @param id                主键
 * @param serial            全局唯一序列号（UUID）
 * @param licenseName       自定义授权名称
 * @param proname           产品标识
 * @param component         组件标识
 * @param version           版本
 * @param licensee          被授权方
 * @param licenseMode       授权模式 center / local / site
 * @param formal            是否正式授权
 * @param expiration        过期时间（never 或日期）
 * @param userinfor         用户信息
 * @param maxInstances      实例配额上限
 * @param maxCpus           CPU 配额（可空）
 * @param maxMemory         内存配额 MB（可空）
 * @param usedInstances     已使用实例数
 * @param remainingInstances 剩余可用实例数
 * @param usedCpus          已使用 CPU
 * @param usedMemory        已使用内存
 * @param bxbFile           原始 XML 文本（防篡改校验）
 * @param status            状态 ACTIVE / DISABLED / EXPIRED
 * @param source            来源 FILE / POOL / GENERATED
 * @param createTime        创建时间
 * @param updateTime        更新时间
 */
public record License(
        Long id,
        String serial,
        String licenseName,
        String proname,
        String component,
        String version,
        String licensee,
        String licenseMode,
        String formal,
        String expiration,
        String userinfor,
        Integer maxInstances,
        Integer maxCpus,
        Integer maxMemory,
        Integer usedInstances,
        Integer remainingInstances,
        Integer usedCpus,
        Integer usedMemory,
        String bxbFile,
        String status,
        String source,
        Instant createTime,
        Instant updateTime
) {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isDisabled() {
        return STATUS_DISABLED.equals(status);
    }

    public boolean isExpired() {
        return STATUS_EXPIRED.equals(status);
    }

    /**
     * 判断授权是否已过期（基于 expiration 字段）。
     * "never" 或空表示永不过期。
     */
    public boolean isPastExpiration() {
        if (expiration == null || expiration.isBlank() || "never".equalsIgnoreCase(expiration)) {
            return false;
        }
        try {
            LocalDate expiryDate = LocalDate.parse(expiration);
            return expiryDate.isBefore(LocalDate.now());
        } catch (Exception e) {
            // 非法日期格式视为未过期（由导入/校验层负责格式校验）
            return false;
        }
    }
}