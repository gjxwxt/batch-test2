package com.example.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端授权管理控制器骨架（共享契约 infra:api-skeleton）。
 *
 * <p>仅定义 API 路径与请求/响应形状，业务逻辑由下游工作包实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/licenses")
public class AdminLicenseController {

    /**
     * 导入授权（JWT）。
     * POST /api/v1/admin/licenses/import —— IAS_AUTH_IMPORT
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importLicense(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    /**
     * 授权列表（JWT）。
     * GET /api/v1/admin/licenses —— IAS_AUTH_LIST
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listLicenses() {
        return ResponseEntity.ok(Map.of("items", java.util.List.of()));
    }

    /**
     * 授权详情（JWT）。
     * GET /api/v1/admin/licenses/{id} —— IAS_AUTH_DETAIL
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLicense(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id));
    }

    /**
     * 授权防篡改校验（JWT）。
     * GET /api/v1/admin/licenses/{id}/verify —— IAS_AUTH_TAMPER_CHECK
     */
    @GetMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyLicense(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "verified", true));
    }

    /**
     * 删除授权（JWT）。
     * DELETE /api/v1/admin/licenses/{id} —— IAS_AUTH_DELETE
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLicense(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }

    /**
     * 禁用授权（JWT）。
     * PUT /api/v1/admin/licenses/{id}/disable —— IAS_AUTH_DISABLE
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<Map<String, Object>> disableLicense(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "disabled", true));
    }
}