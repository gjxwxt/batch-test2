package com.example.app.model;

import java.util.List;

/**
 * 统计趋势 DTO（GET /api/v1/admin/statistics/trend —— IAS_AUTH_STAT_TREND）。
 *
 * <p>按天聚合的实例注册与授权导入时间序列，供趋势折线图渲染。</p>
 *
 * @param days 时间序列桶（按日期升序，每个桶含当日实例注册数与授权导入数）
 */
public record StatisticsTrend(
        List<TrendPoint> days
) {

    /**
     * 单日趋势点。
     *
     * @param date           日期（ISO 8601，yyyy-MM-dd）
     * @param instanceCount  当日实例注册数
     * @param licenseCount   当日授权导入数
     */
    public record TrendPoint(
            String date,
            long instanceCount,
            long licenseCount
    ) {}
}