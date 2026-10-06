package com.example.app.service;

import com.example.app.auth.JwtTokenService;
import com.example.app.exception.AuthException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.AdminUser;
import com.example.app.model.ChangePasswordRequest;
import com.example.app.model.LoginRequest;
import com.example.app.model.LoginResponse;
import com.example.app.repository.AdminUserRepository;
import com.example.app.service.impl.AdminAuthServiceImpl;
import com.example.app.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminAuthService 单元测试（TDD）。
 */
class AdminAuthServiceTest {

    private static final String SEED_HASH =
            "$2a$10$rbkL4Q3ePh.z5w2SGTkvJuQ0P6l5ui7D7fqNxD5RWuVfdW5oEQE/m";

    private AdminUserRepository adminUserRepository;
    private JwtTokenService jwtTokenService;
    private PasswordEncoder passwordEncoder;
    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        jwtTokenService = mock(JwtTokenService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        AuditServiceImpl auditService = new AuditServiceImpl(new com.example.app.repository.AuditLogRepository());
        adminAuthService = new AdminAuthServiceImpl(adminUserRepository, jwtTokenService, passwordEncoder, auditService);
    }

    private AdminUser seedUser() {
        Instant now = Instant.now();
        return new AdminUser(1L, "admin", SEED_HASH, "ACTIVE", null, null, now, now);
    }

    @Test
    @DisplayName("登录成功返回 JWT 令牌")
    void shouldLoginSuccessfully() {
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(seedUser()));
        when(jwtTokenService.issueToken("admin")).thenReturn("jwt-token");

        LoginResponse response = adminAuthService.login(
                new LoginRequest("admin", "Admin@123456"), "127.0.0.1");

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(adminUserRepository).save(any(AdminUser.class));
    }

    @Test
    @DisplayName("密码错误时抛出 AUTH_001")
    void shouldThrowAuth001OnWrongPassword() {
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(seedUser()));

        assertThatThrownBy(() -> adminAuthService.login(
                new LoginRequest("admin", "WrongPass1!"), "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode()).isEqualTo(ErrorCode.AUTH_001));
    }

    @Test
    @DisplayName("用户不存在时抛出 AUTH_001")
    void shouldThrowAuth001WhenUserNotFound() {
        when(adminUserRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.login(
                new LoginRequest("nobody", "Admin@123456"), "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode()).isEqualTo(ErrorCode.AUTH_001));
    }

    @Test
    @DisplayName("修改密码成功")
    void shouldChangePasswordSuccessfully() {
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(seedUser()));

        adminAuthService.changePassword("admin",
                new ChangePasswordRequest("Admin@123456", "NewPass@2026"));

        verify(adminUserRepository).save(any(AdminUser.class));
    }

    @Test
    @DisplayName("旧密码错误时抛出 USER_003")
    void shouldThrowUser003OnWrongOldPassword() {
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(seedUser()));

        assertThatThrownBy(() -> adminAuthService.changePassword("admin",
                new ChangePasswordRequest("WrongOld@1", "NewPass@2026")))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode()).isEqualTo(ErrorCode.USER_003));
    }

    @Test
    @DisplayName("新密码复杂度不足时抛出 USER_004")
    void shouldThrowUser004OnWeakNewPassword() {
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(seedUser()));

        assertThatThrownBy(() -> adminAuthService.changePassword("admin",
                new ChangePasswordRequest("Admin@123456", "weak")))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode()).isEqualTo(ErrorCode.USER_004));
    }
}