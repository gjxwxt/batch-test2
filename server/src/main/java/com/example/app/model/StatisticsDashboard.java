package com.example.app.model;

import java.util.List;

/**
 * Dashboard v2 DTO（GET /api/v1/admin/statistics/dashboard-v2 —— IAS_AUTH_DASHBOARD）。
 *
 * <p>聚合总览、趋势、资源利用率与告警，供管理端仪表盘单次拉取渲染。</p>
 *
 * @param overview         统计总览
 * @param trend            统计趋势
 * @param cpuUtilization   CPU 利用率（0~100，百分比）
 * @param memoryUtilization 内存利用率（0~100，百分比）
 * @param alerts           告警列表
 */
public record StatisticsDashboard(
        StatisticsOverview overview,
        StatisticsTrend trend,
        double cpuUtilization,
        double memoryUtilization,
        List<StatisticsAlert> alerts
) {}