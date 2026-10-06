package com.example.app.controller;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.exception.LicenseException;
import com.example.app.model.FileApplyRequest;
import com.example.app.model.FileApplyResponse;
import com.example.app.model.HeartbeatConfigResponse;
import com.example.app.model.HeartbeatRequest;
import com.example.app.model.HeartbeatResponse;
import com.example.app.model.PublicKeyResponse;
import com.example.app.model.RegisterRequest;
import com.example.app.model.RegisterResponse;
import com.example.app.service.LicenseClientService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 客户端交互控制器（wp-4）切片测试（TDD）。
 *
 * <p>覆盖 5 个端点：public-key / heartbeat-config / register / heartbeat / file-apply，以及错误码映射。</p>
 */
@WebMvcTest(LicenseClientController.class)
@Import(GlobalExceptionHandler.class)
class LicenseClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LicenseClientService licenseClientService;

    @Test
    @DisplayName("GET /api/v1/license/public-key 返回通信公钥")
    void shouldReturnPublicKey() throws Exception {
        when(licenseClientService.getPublicKey())
                .thenReturn(new PublicKeyResponse("RSA", 2048, "base64-public-key"));

        mockMvc.perform(get("/api/v1/license/public-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("RSA"))
                .andExpect(jsonPath("$.keySize").value(2048))
                .andExpect(jsonPath("$.publicKey").value("base64-public-key"));
    }

    @Test
    @DisplayName("GET /api/v1/license/heartbeat-config 返回心跳参数")
    void shouldReturnHeartbeatConfig() throws Exception {
        when(licenseClientService.getHeartbeatConfig())
                .thenReturn(new HeartbeatConfigResponse(30, 3, 90));

        mockMvc.perform(get("/api/v1/license/heartbeat-config")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heartbeatInterval").value(30))
                .andExpect(jsonPath("$.timeoutCount").value(3))
                .andExpect(jsonPath("$.timeoutSeconds").value(90));
    }

    @Test
    @DisplayName("POST /api/v1/license/register 注册成功返回 201")
    void shouldRegisterSuccessfully() throws Exception {
        when(licenseClientService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse("client-uuid-1", "ONLINE", 30, "注册成功"));

        RegisterRequest request = new RegisterRequest(
                "serial-001", "client-uuid-1", "AS", null, null, null,
                "host-1", "192.168.1.10", "AA:BB:CC", "VM", 4, 8192, null, "sig");

        mockMvc.perform(post("/api/v1/license/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instanceId").value("client-uuid-1"))
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    @DisplayName("POST /api/v1/license/register 空 serial 返回 400")
    void shouldRejectBlankSerialOnRegister() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "   ", "client-uuid-1", "AS", null, null, null,
                "host-1", "192.168.1.10", "AA:BB:CC", "VM", 4, 8192, null, "sig");

        mockMvc.perform(post("/api/v1/license/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(licenseClientService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/v1/license/register 授权不存在返回 404 LICENSE_001")
    void shouldReturn404WhenLicenseNotFound() throws Exception {
        when(licenseClientService.register(any(RegisterRequest.class)))
                .thenThrow(new LicenseException(ErrorCode.LICENSE_001, "授权不存在：serial-missing"));

        RegisterRequest request = new RegisterRequest(
                "serial-missing", "client-uuid-1", "AS", null, null, null,
                "host-1", "192.168.1.10", "AA:BB:CC", "VM", 4, 8192, null, "sig");

        mockMvc.perform(post("/api/v1/license/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LICENSE_001"));
    }

    @Test
    @DisplayName("POST /api/v1/license/heartbeat 心跳成功返回 200")
    void shouldHeartbeatSuccessfully() throws Exception {
        when(licenseClientService.heartbeat(any(HeartbeatRequest.class)))
                .thenReturn(new HeartbeatResponse("client-uuid-1", "ONLINE", "2026-10-05T00:00:00Z", "心跳成功"));

        HeartbeatRequest request = new HeartbeatRequest("client-uuid-1", "serial-001", 4, 8192, "sig");

        mockMvc.perform(post("/api/v1/license/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceId").value("client-uuid-1"))
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    @DisplayName("POST /api/v1/license/heartbeat 实例不存在返回 404 INSTANCE_002")
    void shouldReturn404WhenInstanceNotFound() throws Exception {
        when(licenseClientService.heartbeat(any(HeartbeatRequest.class)))
                .thenThrow(new LicenseException(ErrorCode.INSTANCE_002, "实例不存在：missing"));

        HeartbeatRequest request = new HeartbeatRequest("missing", "serial-001", 4, 8192, "sig");

        mockMvc.perform(post("/api/v1/license/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INSTANCE_002"));
    }

    @Test
    @DisplayName("POST /api/v1/license/file-apply 授权文件申请成功返回 200")
    void shouldApplyFileSuccessfully() throws Exception {
        when(licenseClientService.fileApply(any(FileApplyRequest.class)))
                .thenReturn(new FileApplyResponse("serial-001", "<license/>", "local", "授权文件申请成功"));

        FileApplyRequest request = new FileApplyRequest(
                "serial-001", "client-uuid-1", "AS", null, null,
                "host-1", "192.168.1.10", "AA:BB:CC", "sig");

        mockMvc.perform(post("/api/v1/license/file-apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serial").value("serial-001"))
                .andExpect(jsonPath("$.licenseMode").value("local"))
                .andExpect(jsonPath("$.licenseFile").value("<license/>"));
    }

    @Test
    @DisplayName("POST /api/v1/license/file-apply 空 serial 返回 400")
    void shouldRejectBlankSerialOnFileApply() throws Exception {
        FileApplyRequest request = new FileApplyRequest(
                "   ", "client-uuid-1", "AS", null, null,
                "host-1", "192.168.1.10", "AA:BB:CC", "sig");

        mockMvc.perform(post("/api/v1/license/file-apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(licenseClientService, never()).fileApply(any());
    }
}