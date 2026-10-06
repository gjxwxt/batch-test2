package com.example.app.controller;

import com.example.app.model.FileApplyRequest;
import com.example.app.model.FileApplyResponse;
import com.example.app.model.HeartbeatConfigResponse;
import com.example.app.model.HeartbeatRequest;
import com.example.app.model.HeartbeatResponse;
import com.example.app.model.PublicKeyResponse;
import com.example.app.model.RegisterRequest;
import com.example.app.model.RegisterResponse;
import com.example.app.service.LicenseClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端交互控制器（wp-4 客户端交互）。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/license/**（无鉴权）。覆盖：</p>
 * <ul>
 *   <li>GET  /public-key      — 通信公钥分发（req-13 / IAS_AUTH_PUBLIC_KEY）</li>
 *   <li>GET  /heartbeat-config — 心跳参数获取（req-15 / IAS_AUTH_HB_CONFIG）</li>
 *   <li>POST /register        — 实例注册（req-14 / IAS_AUTH_REGISTER）</li>
 *   <li>POST /heartbeat       — 客户端心跳（req-16 / IAS_AUTH_HEARTBEAT）</li>
 *   <li>POST /file-apply      — 授权文件申请（req-37 / IAS_AUTH_FILE_APPLY）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/license")
public class LicenseClientController {

    private final LicenseClientService licenseClientService;

    public LicenseClientController(LicenseClientService licenseClientService) {
        this.licenseClientService = licenseClientService;
    }

    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponse> publicKey() {
        return ResponseEntity.ok(licenseClientService.getPublicKey());
    }

    @GetMapping("/heartbeat-config")
    public ResponseEntity<HeartbeatConfigResponse> heartbeatConfig() {
        return ResponseEntity.ok(licenseClientService.getHeartbeatConfig());
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = licenseClientService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<HeartbeatResponse> heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        return ResponseEntity.ok(licenseClientService.heartbeat(request));
    }

    @PostMapping("/file-apply")
    public ResponseEntity<FileApplyResponse> fileApply(@Valid @RequestBody FileApplyRequest request) {
        return ResponseEntity.ok(licenseClientService.fileApply(request));
    }
}