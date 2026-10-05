package com.example.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端在线实例控制器骨架（共享契约 infra:api-skeleton）。
 *
 * <p>仅定义 API 路径与请求/响应形状，业务逻辑由下游工作包实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/instances")
public class AdminInstanceController {

    /**
     * 在线实例列表（JWT）。
     * GET /api/v1/admin/instances —— IAS_AUTH_INST_LIST
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listInstances() {
        return ResponseEntity.ok(Map.of("items", java.util.List.of()));
    }

    /**
     * 离线实例列表（JWT）。
     * GET /api/v1/admin/instances/offline —— IAS_AUTH_INST_OFFLINE
     */
    @GetMapping("/offline")
    public ResponseEntity<Map<String, Object>> listOfflineInstances() {
        return ResponseEntity.ok(Map.of("items", java.util.List.of()));
    }

    /**
     * 历史实例列表（JWT）。
     * GET /api/v1/admin/instances/history —— IAS_AUTH_INST_HISTORY
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> listHistoryInstances() {
        return ResponseEntity.ok(Map.of("items", java.util.List.of()));
    }

    /**
     * 实例详情（JWT）。
     * GET /api/v1/admin/instances/{id} —— IAS_AUTH_INST_DETAIL
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getInstance(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id));
    }
}