package com.example.app.model;

/**
 * 统计总览 DTO（GET /api/v1/admin/statistics —— IAS_AUTH_STAT_OVERVIEW）。
 *
 * <p>聚合授权、实例、资源配额与审计日志的核心指标，供管理端仪表盘首屏展示。</p>
 *
 * @param licenseCount        授权总数
 * @param activeLicenseCount  启用授权数
 * @param disabledLicenseCount 禁用授权数
 * @param expiredLicenseCount 已过期授权数
 * @param instanceCount       实例总数
 * @param onlineInstanceCount 在线实例数
 * @param offlineInstanceCount 下线实例数
 * @param totalCpus           授权 CPU 配额总和
 * @param totalMemory         授权内存配额总和（MB）
 * @param usedCpus            已使用 CPU 总和
 * @param usedMemory          已使用内存总和（MB）
 * @param auditLogCount       审计日志总数
 */
public record StatisticsOverview(
        long licenseCount,
        long activeLicenseCount,
        long disabledLicenseCount,
        long expiredLicenseCount,
        long instanceCount,
        long onlineInstanceCount,
        long offlineInstanceCount,
        long totalCpus,
        long totalMemory,
        long usedCpus,
        long usedMemory,
        long auditLogCount
) {}