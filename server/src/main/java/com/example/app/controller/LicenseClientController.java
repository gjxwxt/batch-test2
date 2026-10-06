package com.example.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 客户端授权控制器骨架（共享契约 infra:api-skeleton）。
 *
 * <p>仅定义 API 路径与请求/响应形状，业务逻辑由下游工作包实现。</p>
 */
@RestController
@RequestMapping("/api/v1/license")
public class LicenseClientController {

    /**
     * 获取通信公钥（无鉴权）。
     * GET /api/v1/license/public-key —— IAS_AUTH_PUBLIC_KEY
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, Object>> publicKey() {
        return ResponseEntity.ok(Map.of("publicKey", "placeholder"));
    }

    /**
     * 获取心跳配置（无鉴权）。
     * GET /api/v1/license/heartbeat-config —— IAS_AUTH_HB_CONFIG
     */
    @GetMapping("/heartbeat-config")
    public ResponseEntity<Map<String, Object>> heartbeatConfig() {
        return ResponseEntity.ok(Map.of());
    }

    /**
     * 客户端注册（无鉴权）。
     * POST /api/v1/license/register —— IAS_AUTH_REGISTER
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    /**
     * 客户端心跳（无鉴权）。
     * POST /api/v1/license/heartbeat —— IAS_AUTH_HEARTBEAT
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    /**
     * 授权文件申请（无鉴权）。
     * POST /api/v1/license/file-apply —— IAS_AUTH_FILE_APPLY
     */
    @PostMapping("/file-apply")
    public ResponseEntity<Map<String, Object>> fileApply(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}