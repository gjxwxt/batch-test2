package com.example.app.controller;

import com.example.app.auth.JwtTokenService;
import com.example.app.exception.AuthException;
import com.example.app.exception.ErrorCode;
import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.model.ChangePasswordRequest;
import com.example.app.model.LoginRequest;
import com.example.app.model.LoginResponse;
import com.example.app.service.AdminAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminAuthController 切片测试（TDD）。
 */
@WebMvcTest(AdminAuthController.class)
@Import(GlobalExceptionHandler.class)
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminAuthService adminAuthService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("POST /api/v1/admin/login 成功返回 JWT 令牌")
    void shouldLoginSuccessfully() throws Exception {
        LoginResponse response = new LoginResponse("jwt-token", "admin", "Bearer");
        when(adminAuthService.login(any(LoginRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "Admin@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/login 用户名或密码错误返回 401 AUTH_001")
    void shouldReturn401OnWrongCredentials() throws Exception {
        when(adminAuthService.login(any(LoginRequest.class), any()))
                .thenThrow(new AuthException(ErrorCode.AUTH_001, "用户名或密码错误"));

        mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "WrongPass1!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/login 参数缺失返回 400")
    void shouldReturn400OnMissingParams() throws Exception {
        mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(adminAuthService, never()).login(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/password 修改密码成功返回 200")
    void shouldChangePasswordSuccessfully() throws Exception {
        when(jwtTokenService.validateToken("valid-token")).thenReturn("admin");
        doNothing().when(adminAuthService).changePassword(eq("admin"), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/admin/password")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Admin@123456", "NewPass@2026"))))
                .andExpect(status().isOk());

        verify(adminAuthService).changePassword(eq("admin"), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/password 旧密码错误返回 400 USER_003")
    void shouldReturn400OnWrongOldPassword() throws Exception {
        when(jwtTokenService.validateToken("valid-token")).thenReturn("admin");
        doThrow(new AuthException(ErrorCode.USER_003, "旧密码错误"))
                .when(adminAuthService).changePassword(eq("admin"), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/admin/password")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("WrongOld@1", "NewPass@2026"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_003"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/password 缺少令牌返回 401 AUTH_002")
    void shouldReturn401WhenTokenMissing() throws Exception {
        mockMvc.perform(put("/api/v1/admin/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Admin@123456", "NewPass@2026"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }
}