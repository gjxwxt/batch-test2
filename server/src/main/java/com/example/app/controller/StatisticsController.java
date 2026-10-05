package com.example.app.controller;

import com.example.app.exception.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享契约 · API 骨架（infra:api-skeleton）— 统计与仪表盘端点（O2 纳入）。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/admin/statistics/**。业务逻辑由并行工作包 wp-8（统计与仪表盘）实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/statistics")
public class StatisticsController {

    @GetMapping
    public ResponseEntity<ApiErrorResponse> overview() {
        // TODO(wp-8): IAS_AUTH_STAT_OVERVIEW — 统计概览
        throw new UnsupportedOperationException("wp-8: IAS_AUTH_STAT_OVERVIEW not yet implemented");
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiErrorResponse> trend() {
        // TODO(wp-8): IAS_AUTH_STAT_TREND — 趋势数据
        throw new UnsupportedOperationException("wp-8: IAS_AUTH_STAT_TREND not yet implemented");
    }

    @GetMapping("/dashboard-v2")
    public ResponseEntity<ApiErrorResponse> dashboard() {
        // TODO(wp-8): IAS_AUTH_DASHBOARD — 仪表盘与告警
        throw new UnsupportedOperationException("wp-8: IAS_AUTH_DASHBOARD not yet implemented");
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiErrorResponse> alerts() {
        // TODO(wp-8): IAS_AUTH_ALERTS — 告警
        throw new UnsupportedOperationException("wp-8: IAS_AUTH_ALERTS not yet implemented");
    }

    @GetMapping("/export")
    public ResponseEntity<ApiErrorResponse> export() {
        // TODO(wp-8): IAS_AUTH_STAT_EXPORT — 统计导出
        throw new UnsupportedOperationException("wp-8: IAS_AUTH_STAT_EXPORT not yet implemented");
    }
}