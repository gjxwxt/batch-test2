package com.example.app.controller;

import com.example.app.exception.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享契约 · API 骨架（infra:api-skeleton）— 管理员认证端点。
 *
 * <p>路径冻结（6.2 映射）：POST /api/v1/admin/login、PUT /api/v1/admin/password。
 * 业务逻辑由并行工作包 wp-2（服务端启动与认证）实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

    @PostMapping("/login")
    public ResponseEntity<ApiErrorResponse> login() {
        // TODO(wp-2): IAS_AUTH_LOGIN — 管理员登录
        throw new UnsupportedOperationException("wp-2: IAS_AUTH_LOGIN not yet implemented");
    }

    @PutMapping("/password")
    public ResponseEntity<ApiErrorResponse> changePassword() {
        // TODO(wp-2): IAS_AUTH_CHANGE_PWD — 修改管理员密码
        throw new UnsupportedOperationException("wp-2: IAS_AUTH_CHANGE_PWD not yet implemented");
    }
}