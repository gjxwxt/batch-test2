package com.example.app.controller;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.exception.LicenseException;
import com.example.app.model.HistoryInstance;
import com.example.app.model.Instance;
import com.example.app.service.InstanceAdminService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 实例监控控制器（wp-5）切片测试（TDD）。
 *
 * <p>覆盖 4 个端点：/instances、/instances/offline、/instances/history、/instances/{id}，
 * 以及 INSTANCE_002 错误码映射。</p>
 */
@WebMvcTest(InstanceAdminController.class)
@Import(GlobalExceptionHandler.class)
class InstanceAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstanceAdminService instanceAdminService;

    private Instance sampleInstance(Long id, String instanceId, String status) {
        Instant now = Instant.now();
        return new Instance(
                id, instanceId, 1L, "uuid-" + instanceId, "InforSuiteAS", "AS",
                "9.0", "Standard", "host-" + instanceId, "10.0.0.1", "AA:BB:CC:DD:EE:FF",
                "VM", 4, 8192, null, status,
                now, now, status.equals(Instance.STATUS_OFFLINE) ? now : null, now, now
        );
    }

    @Test
    @DisplayName("GET /api/v1/admin/instances 返回在线实例列表")
    void shouldReturnOnlineInstances() throws Exception {
        when(instanceAdminService.listOnlineInstances())
                .thenReturn(List.of(sampleInstance(1L, "inst-1", Instance.STATUS_ONLINE)));

        mockMvc.perform(get("/api/v1/admin/instances").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instanceId").value("inst-1"))
                .andExpect(jsonPath("$[0].status").value("ONLINE"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/instances/offline 返回下线实例列表")
    void shouldReturnOfflineInstances() throws Exception {
        when(instanceAdminService.listOfflineInstances())
                .thenReturn(List.of(sampleInstance(2L, "inst-2", Instance.STATUS_OFFLINE)));

        mockMvc.perform(get("/api/v1/admin/instances/offline").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instanceId").value("inst-2"))
                .andExpect(jsonPath("$[0].status").value("OFFLINE"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/instances/history 返回历史实例列表")
    void shouldReturnHistoryInstances() throws Exception {
        Instant now = Instant.now();
        HistoryInstance history = new HistoryInstance(
                1L, "hist-1", 1L, "uuid-hist-1", "InforSuiteAS", "AS", "9.0", "Standard",
                "host-hist-1", "10.0.0.1", "AA:BB:CC:DD:EE:FF", "VM", 4, 8192, null,
                HistoryInstance.STATUS_OFFLINE, now, now, now, now, now, now);
        when(instanceAdminService.listHistoryInstances()).thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/admin/instances/history").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instanceId").value("hist-1"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/instances/{id} 返回实例详情")
    void shouldReturnInstanceDetail() throws Exception {
        when(instanceAdminService.getInstance(1L))
                .thenReturn(sampleInstance(1L, "inst-1", Instance.STATUS_ONLINE));

        mockMvc.perform(get("/api/v1/admin/instances/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceId").value("inst-1"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/instances/{id} 不存在时返回 404 与 INSTANCE_002")
    void shouldReturnNotFoundWhenInstanceMissing() throws Exception {
        when(instanceAdminService.getInstance(999L))
                .thenThrow(new LicenseException(ErrorCode.INSTANCE_002, "实例不存在：999"));

        mockMvc.perform(get("/api/v1/admin/instances/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INSTANCE_002"));
    }
}