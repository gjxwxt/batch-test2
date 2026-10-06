package com.example.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端系统配置控制器骨架（共享契约 infra:api-skeleton）。
 *
 * <p>仅定义 API 路径与请求/响应形状，业务逻辑由下游工作包实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/config")
public class AdminConfigController {

    /**
     * 查看系统配置（JWT）。
     * GET /api/v1/admin/config —— IAS_AUTH_CONFIG_VIEW
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> viewConfig() {
        return ResponseEntity.ok(Map.of());
    }

    /**
     * 更新心跳配置（JWT）。
     * PUT /api/v1/admin/config/heartbeat —— IAS_AUTH_CONFIG_HB
     */
    @PutMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> updateHeartbeatConfig(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    /**
     * 重载配置（JWT）。
     * POST /api/v1/admin/config/reload —— IAS_AUTH_CONFIG_RELOAD
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadConfig() {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}