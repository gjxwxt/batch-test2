package com.example.app.service;

import com.example.app.model.ChangePasswordRequest;
import com.example.app.model.LoginRequest;
import com.example.app.model.LoginResponse;

/**
 * 管理员认证服务（登录 / 修改密码）。
 */
public interface AdminAuthService {

    /**
     * 管理员登录（IAS_AUTH_LOGIN）。成功返回 JWT 令牌。
     */
    LoginResponse login(LoginRequest request, String clientIp);

    /**
     * 修改密码（IAS_AUTH_CHANGE_PWD）。需携带有效 JWT。
     */
    void changePassword(String username, ChangePasswordRequest request);
}