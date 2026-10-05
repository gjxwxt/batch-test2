package com.example.app.controller;

import com.example.app.exception.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享契约 · API 骨架（infra:api-skeleton）— 审计与系统配置端点。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/admin/audit-logs、/api/v1/admin/config/**。
 * 业务逻辑由并行工作包 wp-6（审计与系统配置）实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AuditConfigController {

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiErrorResponse> listAuditLogs() {
        // TODO(wp-6): IAS_AUTH_AUDIT — 审计日志查询
        throw new UnsupportedOperationException("wp-6: IAS_AUTH_AUDIT not yet implemented");
    }

    @GetMapping("/config")
    public ResponseEntity<ApiErrorResponse> viewConfig() {
        // TODO(wp-6): IAS_AUTH_CONFIG_VIEW — 系统配置查看
        throw new UnsupportedOperationException("wp-6: IAS_AUTH_CONFIG_VIEW not yet implemented");
    }

    @PutMapping("/config/heartbeat")
    public ResponseEntity<ApiErrorResponse> updateHeartbeatConfig() {
        // TODO(wp-6): IAS_AUTH_CONFIG_HB — 心跳配置修改
        throw new UnsupportedOperationException("wp-6: IAS_AUTH_CONFIG_HB not yet implemented");
    }

    @PostMapping("/config/reload")
    public ResponseEntity<ApiErrorResponse> reloadConfig() {
        // TODO(wp-6): IAS_AUTH_CONFIG_RELOAD — 配置 reload
        throw new UnsupportedOperationException("wp-6: IAS_AUTH_CONFIG_RELOAD not yet implemented");
    }
}