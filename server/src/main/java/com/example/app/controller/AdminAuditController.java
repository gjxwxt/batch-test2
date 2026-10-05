package com.example.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端审计日志控制器骨架（共享契约 infra:api-skeleton）。
 *
 * <p>仅定义 API 路径与请求/响应形状，业务逻辑由下游工作包实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditController {

    /**
     * 审计日志列表（JWT）。
     * GET /api/v1/admin/audit-logs —— IAS_AUTH_AUDIT
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAuditLogs() {
        return ResponseEntity.ok(Map.of("items", java.util.List.of()));
    }
}