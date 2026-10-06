package com.example.app.service.impl;

import com.example.app.auth.JwtTokenService;
import com.example.app.exception.AuthException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.AdminUser;
import com.example.app.model.AuditOperationType;
import com.example.app.model.ChangePasswordRequest;
import com.example.app.model.LoginRequest;
import com.example.app.model.LoginResponse;
import com.example.app.repository.AdminUserRepository;
import com.example.app.service.AdminAuthService;
import com.example.app.service.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * 管理员认证服务实现。
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final Pattern PASSWORD_COMPLEXITY =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final AdminUserRepository adminUserRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminAuthServiceImpl(AdminUserRepository adminUserRepository,
                                JwtTokenService jwtTokenService,
                                PasswordEncoder passwordEncoder,
                                AuditService auditService) {
        this.adminUserRepository = adminUserRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public LoginResponse login(LoginRequest request, String clientIp) {
        AdminUser user = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_001, "用户名或密码错误"));

        if (!user.isActive()) {
            throw new AuthException(ErrorCode.AUTH_001, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new AuthException(ErrorCode.AUTH_001, "用户名或密码错误");
        }

        // 更新最后登录时间与 IP
        AdminUser updated = new AdminUser(
                user.id(),
                user.username(),
                user.passwordHash(),
                user.status(),
                Instant.now(),
                clientIp,
                user.createTime(),
                Instant.now()
        );
        adminUserRepository.save(updated);

        String token = jwtTokenService.issueToken(user.username());
        auditService.record(AuditOperationType.LOGIN, user.username(), clientIp,
                user.username(), "SUCCESS", "管理员登录成功");
        return new LoginResponse(token, user.username(), "Bearer");
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_001, "用户不存在"));

        if (!passwordEncoder.matches(request.oldPassword(), user.passwordHash())) {
            throw new AuthException(ErrorCode.USER_003, "旧密码错误");
        }

        if (!PASSWORD_COMPLEXITY.matcher(request.newPassword()).matches()) {
            throw new AuthException(ErrorCode.USER_004, "新密码复杂度不足");
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        AdminUser updated = new AdminUser(
                user.id(),
                user.username(),
                newHash,
                user.status(),
                user.lastLoginTime(),
                user.lastLoginIp(),
                user.createTime(),
                Instant.now()
        );
        adminUserRepository.save(updated);
        auditService.record(AuditOperationType.CHANGE_PASSWORD, username, null,
                username, "SUCCESS", "管理员修改密码成功");
    }
}