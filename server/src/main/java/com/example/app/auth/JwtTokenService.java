package com.example.app.auth;

import com.example.app.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 令牌签发与校验服务。
 *
 * <p>管理接口携带 {@code Authorization: Bearer <JWT>}；令牌过期 → AUTH_002（HTTP 401）。
 */
@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long ttlMinutes;

    public JwtTokenService(
            @Value("${auth.jwt.secret:InforSuiteAuthCenterJwtSecretKey2026ChangeMe!}") String secret,
            @Value("${auth.jwt.ttl-minutes:120}") long ttlMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * 为指定用户名签发 JWT 令牌。
     */
    public String issueToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ttlMinutes * 60);
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验令牌并返回用户名。令牌无效或过期时抛出 {@link JwtAuthenticationException}。
     */
    public String validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            throw new JwtAuthenticationException("登录已过期或令牌无效", e);
        }
    }
}