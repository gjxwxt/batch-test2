package com.example.app.controller;

import com.example.app.exception.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享契约 · API 骨架（infra:api-skeleton）— 客户端 SDK 端点（无鉴权）。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/license/**。业务逻辑由并行工作包 wp-4（客户端交互）实现。</p>
 */
@RestController
@RequestMapping("/api/v1/license")
public class LicenseClientController {

    @GetMapping("/public-key")
    public ResponseEntity<ApiErrorResponse> publicKey() {
        // TODO(wp-4): IAS_AUTH_PUBLIC_KEY — RSA 公钥分发
        throw new UnsupportedOperationException("wp-4: IAS_AUTH_PUBLIC_KEY not yet implemented");
    }

    @GetMapping("/heartbeat-config")
    public ResponseEntity<ApiErrorResponse> heartbeatConfig() {
        // TODO(wp-4): IAS_AUTH_HB_CONFIG — 心跳参数获取
        throw new UnsupportedOperationException("wp-4: IAS_AUTH_HB_CONFIG not yet implemented");
    }

    @PostMapping("/register")
    public ResponseEntity<ApiErrorResponse> register() {
        // TODO(wp-4): IAS_AUTH_REGISTER — 实例注册
        throw new UnsupportedOperationException("wp-4: IAS_AUTH_REGISTER not yet implemented");
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiErrorResponse> heartbeat() {
        // TODO(wp-4): IAS_AUTH_HEARTBEAT — 客户端心跳
        throw new UnsupportedOperationException("wp-4: IAS_AUTH_HEARTBEAT not yet implemented");
    }

    @PostMapping("/file-apply")
    public ResponseEntity<ApiErrorResponse> fileApply() {
        // TODO(wp-4): IAS_AUTH_FILE_APPLY — 授权文件申请（local/site）
        throw new UnsupportedOperationException("wp-4: IAS_AUTH_FILE_APPLY not yet implemented");
    }
}