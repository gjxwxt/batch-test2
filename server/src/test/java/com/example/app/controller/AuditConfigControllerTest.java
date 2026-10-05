package com.example.app.controller;

import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.model.AuditLog;
import com.example.app.model.AuditLogQuery;
import com.example.app.model.AuditOperationType;
import com.example.app.model.HeartbeatConfigRequest;
import com.example.app.model.SystemConfig;
import com.example.app.service.AuditService;
import com.example.app.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditConfigController.class)
@Import(GlobalExceptionHandler.class)
class AuditConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditService auditService;

    @MockBean
    private SystemConfigService systemConfigService;

    @Test
    @DisplayName("GET /api/v1/admin/audit-logs returns audit logs with 200 OK")
    void shouldReturnAuditLogs() throws Exception {
        AuditLog log = new AuditLog("log-1", AuditOperationType.LOGIN, "管理员登录",
                "admin", "192.168.1.10", "admin-1", "SUCCESS", "登录成功", Instant.now());
        when(auditService.query(any(AuditLogQuery.class))).thenReturn(List.of(log));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value("log-1"))
                .andExpect(jsonPath("$[0].operationType").value("LOGIN"))
                .andExpect(jsonPath("$[0].operator").value("admin"));

        verify(auditService, times(1)).query(any(AuditLogQuery.class));
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit-logs filters by operationType query param")
    void shouldFilterAuditLogsByOperationType() throws Exception {
        when(auditService.query(any(AuditLogQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("operationType", "LOGIN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(auditService, times(1)).query(any(AuditLogQuery.class));
    }

    @Test
    @DisplayName("GET /api/v1/admin/config returns system config with 200 OK")
    void shouldReturnSystemConfig() throws Exception {
        SystemConfig config = new SystemConfig(1L, "heartbeat.interval", "30", "心跳间隔（秒）", Instant.now());
        when(systemConfigService.getAllConfigs()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/v1/admin/config")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].configKey").value("heartbeat.interval"))
                .andExpect(jsonPath("$[0].configValue").value("30"));

        verify(systemConfigService, times(1)).getAllConfigs();
    }

    @Test
    @DisplayName("PUT /api/v1/admin/config/heartbeat updates heartbeat config with 200 OK")
    void shouldUpdateHeartbeatConfig() throws Exception {
        HeartbeatConfigRequest request = new HeartbeatConfigRequest(60, 5);
        SystemConfig interval = new SystemConfig(1L, "heartbeat.interval", "60", "心跳间隔（秒）", Instant.now());
        SystemConfig timeout = new SystemConfig(2L, "heartbeat.timeout_multiplier", "5", "心跳超时倍数", Instant.now());
        when(systemConfigService.updateHeartbeatConfig(any(HeartbeatConfigRequest.class)))
                .thenReturn(List.of(interval, timeout));

        mockMvc.perform(put("/api/v1/admin/config/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].configValue").value("60"))
                .andExpect(jsonPath("$[1].configValue").value("5"));

        verify(systemConfigService, times(1)).updateHeartbeatConfig(any(HeartbeatConfigRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/config/heartbeat returns 400 when interval invalid")
    void shouldRejectInvalidHeartbeatInterval() throws Exception {
        HeartbeatConfigRequest request = new HeartbeatConfigRequest(0, 5);

        mockMvc.perform(put("/api/v1/admin/config/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(systemConfigService, never()).updateHeartbeatConfig(any());
    }

    @Test
    @DisplayName("POST /api/v1/admin/config/reload reloads config with 200 OK")
    void shouldReloadConfig() throws Exception {
        SystemConfig config = new SystemConfig(1L, "heartbeat.interval", "30", "心跳间隔（秒）", Instant.now());
        when(systemConfigService.reloadConfig()).thenReturn(List.of(config));

        mockMvc.perform(post("/api/v1/admin/config/reload")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].configKey").value("heartbeat.interval"));

        verify(systemConfigService, times(1)).reloadConfig();
    }
}