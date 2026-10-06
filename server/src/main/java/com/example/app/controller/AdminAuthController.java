package com.example.app.controller;

import com.example.app.model.ChangePasswordRequest;
import com.example.app.model.LoginRequest;
import com.example.app.model.LoginResponse;
import com.example.app.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员认证控制器（IAS_AUTH_LOGIN / IAS_AUTH_CHANGE_PWD）。
 *
 * <ul>
 *   <li>POST /api/v1/admin/login — 登录，无鉴权，返回 JWT 令牌</li>
 *   <li>PUT /api/v1/admin/password — 修改密码，JWT 鉴权</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        LoginResponse response = adminAuthService.login(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestAttribute("authenticatedUsername") String username,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        adminAuthService.changePassword(username, request);
        return ResponseEntity.ok().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}