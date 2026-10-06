package com.example.app.controller;

import com.example.app.model.AuditLog;
import com.example.app.model.AuditLogQuery;
import com.example.app.model.AuditOperationType;
import com.example.app.model.HeartbeatConfigRequest;
import com.example.app.model.SystemConfig;
import com.example.app.service.AuditService;
import com.example.app.service.SystemConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计日志与系统配置控制器。
 * 覆盖契约端点：
 * - GET  /api/v1/admin/audit-logs        (IAS_AUTH_AUDIT)
 * - GET  /api/v1/admin/config            (IAS_AUTH_CONFIG_VIEW)
 * - PUT  /api/v1/admin/config/heartbeat  (IAS_AUTH_CONFIG_HB)
 * - POST /api/v1/admin/config/reload     (IAS_AUTH_CONFIG_RELOAD)
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AuditConfigController {

    private final AuditService auditService;
    private final SystemConfigService systemConfigService;

    public AuditConfigController(AuditService auditService, SystemConfigService systemConfigService) {
        this.auditService = auditService;
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> listAuditLogs(
            @RequestParam(required = false) AuditOperationType operationType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String targetId) {
        AuditLogQuery query = new AuditLogQuery(operationType, operator, result, targetId);
        return ResponseEntity.ok(auditService.query(query));
    }

    @GetMapping("/config")
    public ResponseEntity<List<SystemConfig>> getConfig() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }

    @PutMapping("/config/heartbeat")
    public ResponseEntity<List<SystemConfig>> updateHeartbeatConfig(
            @Valid @RequestBody HeartbeatConfigRequest request) {
        return ResponseEntity.ok(systemConfigService.updateHeartbeatConfig(request));
    }

    @PostMapping("/config/reload")
    public ResponseEntity<List<SystemConfig>> reloadConfig() {
        return ResponseEntity.ok(systemConfigService.reloadConfig());
    }
}