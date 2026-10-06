package com.example.app.auth;

import com.example.app.exception.JwtAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器。
 *
 * <p>保护 {@code /api/v1/admin/**}（除 login 外）。请求需携带
 * {@code Authorization: Bearer <JWT>}；令牌缺失/无效/过期 → AUTH_002（HTTP 401）。
 * 校验通过后将用户名写入 request attribute {@code authenticatedUsername}。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    public JwtAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new JwtAuthenticationException("缺少或非法的 Authorization 头");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        String username = jwtTokenService.validateToken(token);
        request.setAttribute("authenticatedUsername", username);
        return true;
    }
}