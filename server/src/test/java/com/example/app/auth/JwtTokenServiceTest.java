package com.example.app.auth;

import com.example.app.exception.JwtAuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenService 单元测试（TDD）。
 */
class JwtTokenServiceTest {

    private final JwtTokenService jwtTokenService =
            new JwtTokenService("InforSuiteAuthCenterJwtSecretKey2026ChangeMe!", 120);

    @Test
    @DisplayName("签发令牌后可校验并还原用户名")
    void shouldIssueAndValidateToken() {
        String token = jwtTokenService.issueToken("admin");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.validateToken(token)).isEqualTo("admin");
    }

    @Test
    @DisplayName("无效令牌校验失败抛出 JwtAuthenticationException")
    void shouldRejectInvalidToken() {
        assertThatThrownBy(() -> jwtTokenService.validateToken("invalid.token.value"))
                .isInstanceOf(JwtAuthenticationException.class);
    }

    @Test
    @DisplayName("过期令牌校验失败抛出 JwtAuthenticationException")
    void shouldRejectExpiredToken() {
        JwtTokenService shortTtl = new JwtTokenService("InforSuiteAuthCenterJwtSecretKey2026ChangeMe!", 0);
        String token = shortTtl.issueToken("admin");

        assertThatThrownBy(() -> shortTtl.validateToken(token))
                .isInstanceOf(JwtAuthenticationException.class);
    }
}