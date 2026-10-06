package com.example.app.controller;

import com.example.app.model.StatisticsAlert;
import com.example.app.model.StatisticsDashboard;
import com.example.app.model.StatisticsOverview;
import com.example.app.model.StatisticsTrend;
import com.example.app.service.StatisticsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端统计控制器（wp-8 统计与仪表盘）。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/admin/statistics/**（JWT 鉴权）。覆盖：</p>
 * <ul>
 *   <li>GET /statistics        — 统计总览（req-29 / IAS_AUTH_STAT_OVERVIEW）</li>
 *   <li>GET /statistics/trend  — 统计趋势（req-34 / IAS_AUTH_STAT_TREND）</li>
 *   <li>GET /statistics/dashboard-v2 — Dashboard v2（req-35 / IAS_AUTH_DASHBOARD）</li>
 *   <li>GET /statistics/alerts — 告警列表（req-36 / IAS_AUTH_ALERTS）</li>
 *   <li>GET /statistics/export — 统计导出（req-36 / IAS_AUTH_STAT_EXPORT）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/statistics")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    public AdminStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 统计总览（JWT）。
     * GET /api/v1/admin/statistics —— IAS_AUTH_STAT_OVERVIEW
     */
    @GetMapping
    public ResponseEntity<StatisticsOverview> overview() {
        return ResponseEntity.ok(statisticsService.overview());
    }

    /**
     * 统计趋势（JWT）。
     * GET /api/v1/admin/statistics/trend —— IAS_AUTH_STAT_TREND
     */
    @GetMapping("/trend")
    public ResponseEntity<StatisticsTrend> trend(
            @RequestParam(value = "days", required = false, defaultValue = "30") int days) {
        return ResponseEntity.ok(statisticsService.trend(days));
    }

    /**
     * Dashboard v2（JWT）。
     * GET /api/v1/admin/statistics/dashboard-v2 —— IAS_AUTH_DASHBOARD
     */
    @GetMapping("/dashboard-v2")
    public ResponseEntity<StatisticsDashboard> dashboardV2() {
        return ResponseEntity.ok(statisticsService.dashboardV2());
    }

    /**
     * 告警列表（JWT）。
     * GET /api/v1/admin/statistics/alerts —— IAS_AUTH_ALERTS
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<StatisticsAlert>> alerts() {
        return ResponseEntity.ok(statisticsService.alerts());
    }

    /**
     * 统计导出（JWT）。
     * GET /api/v1/admin/statistics/export —— IAS_AUTH_STAT_EXPORT
     */
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> export() {
        return ResponseEntity.ok(statisticsService.export());
    }
}