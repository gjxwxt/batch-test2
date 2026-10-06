package com.example.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端统计控制器骨架（共享契约 infra:api-skeleton）。
 *
 * <p>仅定义 API 路径与请求/响应形状，业务逻辑由下游工作包实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/statistics")
public class AdminStatisticsController {

    /**
     * 统计总览（JWT）。
     * GET /api/v1/admin/statistics —— IAS_AUTH_STAT_OVERVIEW
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(Map.of());
    }

    /**
     * 统计趋势（JWT）。
     * GET /api/v1/admin/statistics/trend —— IAS_AUTH_STAT_TREND
     */
    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> trend() {
        return ResponseEntity.ok(Map.of());
    }

    /**
     * Dashboard v2（JWT）。
     * GET /api/v1/admin/statistics/dashboard-v2 —— IAS_AUTH_DASHBOARD
     */
    @GetMapping("/dashboard-v2")
    public ResponseEntity<Map<String, Object>> dashboardV2() {
        return ResponseEntity.ok(Map.of());
    }

    /**
     * 告警列表（JWT）。
     * GET /api/v1/admin/statistics/alerts —— IAS_AUTH_ALERTS
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> alerts() {
        return ResponseEntity.ok(Map.of("items", java.util.List.of()));
    }

    /**
     * 统计导出（JWT）。
     * GET /api/v1/admin/statistics/export —— IAS_AUTH_STAT_EXPORT
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export() {
        return ResponseEntity.ok(Map.of());
    }
}