package com.example.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端认证控制器骨架（共享契约 infra:api-skeleton）。
 *
 * <p>仅定义 API 路径与请求/响应形状，业务逻辑由下游工作包实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

    /**
     * 管理员登录（无鉴权）。
     * POST /api/v1/admin/login —— IAS_AUTH_LOGIN
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("token", "placeholder"));
    }

    /**
     * 修改管理员密码（JWT）。
     * PUT /api/v1/admin/password —— IAS_AUTH_CHANGE_PWD
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}