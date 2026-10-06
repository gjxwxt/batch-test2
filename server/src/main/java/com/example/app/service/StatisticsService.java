package com.example.app.service;

import com.example.app.model.StatisticsAlert;
import com.example.app.model.StatisticsDashboard;
import com.example.app.model.StatisticsOverview;
import com.example.app.model.StatisticsTrend;

import java.util.List;

/**
 * 统计与仪表盘服务（wp-8）。
 *
 * <p>覆盖需求锚点：</p>
 * <ul>
 *   <li>req-29 统计总览（IAS_AUTH_STAT_OVERVIEW）</li>
 *   <li>req-34 统计趋势（IAS_AUTH_STAT_TREND）</li>
 *   <li>req-35 Dashboard v2（IAS_AUTH_DASHBOARD）</li>
 *   <li>req-36 告警列表（IAS_AUTH_ALERTS）与统计导出（IAS_AUTH_STAT_EXPORT）</li>
 * </ul>
 *
 * <p>聚合查询采用单次遍历（single-pass）实现，避免对授权/实例/历史/审计多表做 N+1 查询，
 * 保证大数据量下的聚合查询性能（聚合查询性能回归）。</p>
 */
public interface StatisticsService {

    /**
     * 统计总览（req-29 / IAS_AUTH_STAT_OVERVIEW）。
     * 聚合授权、实例、资源配额与审计日志核心指标。
     */
    StatisticsOverview overview();

    /**
     * 统计趋势（req-34 / IAS_AUTH_STAT_TREND）。
     * 返回最近 {@code days} 天的实例注册与授权导入时间序列。
     *
     * @param days 趋势天数（1~365，非法值回退默认 30）
     */
    StatisticsTrend trend(int days);

    /**
     * Dashboard v2（req-35 / IAS_AUTH_DASHBOARD）。
     * 聚合总览、趋势、资源利用率与告警。
     */
    StatisticsDashboard dashboardV2();

    /**
     * 告警列表（req-36 / IAS_AUTH_ALERTS）。
     * 返回即将过期授权、配额告警、离线实例等异常项。
     */
    List<StatisticsAlert> alerts();

    /**
     * 统计导出（req-36 / IAS_AUTH_STAT_EXPORT）。
     * 返回统计总览的 CSV 文本。
     */
    String export();
}