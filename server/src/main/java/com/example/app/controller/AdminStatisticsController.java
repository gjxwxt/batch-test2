package com.example.app.controller;

import com.example.app.model.AuditOperationType;
import com.example.app.model.StatisticsAlert;
import com.example.app.model.StatisticsDashboard;
import com.example.app.model.StatisticsOverview;
import com.example.app.model.StatisticsTrend;
import com.example.app.service.AuditService;
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
    private final AuditService auditService;

    public AdminStatisticsController(StatisticsService statisticsService,
                                     AuditService auditService) {
        this.statisticsService = statisticsService;
        this.auditService = auditService;
    }

    /**
     * 统计总览（JWT）。
     * GET /api/v1/admin/statistics —— IAS_AUTH_STAT_OVERVIEW
     */
    @GetMapping
    public ResponseEntity<StatisticsOverview> overview() {
        StatisticsOverview result = statisticsService.overview();
        auditService.record(AuditOperationType.STATISTICS_QUERY, "admin", null,
                null, "SUCCESS", "查询统计总览");
        return ResponseEntity.ok(result);
    }

    /**
     * 统计趋势（JWT）。
     * GET /api/v1/admin/statistics/trend —— IAS_AUTH_STAT_TREND
     */
    @GetMapping("/trend")
    public ResponseEntity<StatisticsTrend> trend(
            @RequestParam(value = "days", required = false, defaultValue = "30") int days) {
        StatisticsTrend result = statisticsService.trend(days);
        auditService.record(AuditOperationType.STATISTICS_QUERY, "admin", null,
                null, "SUCCESS", "查询统计趋势，days=" + days);
        return ResponseEntity.ok(result);
    }

    /**
     * Dashboard v2（JWT）。
     * GET /api/v1/admin/statistics/dashboard-v2 —— IAS_AUTH_DASHBOARD
     */
    @GetMapping("/dashboard-v2")
    public ResponseEntity<StatisticsDashboard> dashboardV2() {
        StatisticsDashboard result = statisticsService.dashboardV2();
        auditService.record(AuditOperationType.STATISTICS_QUERY, "admin", null,
                null, "SUCCESS", "查询 Dashboard v2");
        return ResponseEntity.ok(result);
    }

    /**
     * 告警列表（JWT）。
     * GET /api/v1/admin/statistics/alerts —— IAS_AUTH_ALERTS
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<StatisticsAlert>> alerts() {
        List<StatisticsAlert> result = statisticsService.alerts();
        auditService.record(AuditOperationType.STATISTICS_QUERY, "admin", null,
                null, "SUCCESS", "查询告警列表，共 " + result.size() + " 条");
        return ResponseEntity.ok(result);
    }

    /**
     * 统计导出（JWT）。
     * GET /api/v1/admin/statistics/export —— IAS_AUTH_STAT_EXPORT
     */
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> export() {
        String result = statisticsService.export();
        auditService.record(AuditOperationType.STATISTICS_QUERY, "admin", null,
                null, "SUCCESS", "导出统计报表");
        return ResponseEntity.ok(result);
    }
}