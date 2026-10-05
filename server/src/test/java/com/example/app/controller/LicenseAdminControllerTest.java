package com.example.app.controller;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.exception.LicenseException;
import com.example.app.model.License;
import com.example.app.model.LicenseImportRequest;
import com.example.app.model.LicenseSummary;
import com.example.app.model.LicenseVerifyResponse;
import com.example.app.service.LicenseAdminService;
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

/**
 * 授权管理控制器（wp-3）切片测试（TDD）。
 *
 * <p>覆盖 6 个端点：import / list / detail / verify / delete / disable，以及错误码映射。</p>
 */
@WebMvcTest(LicenseAdminController.class)
@Import(GlobalExceptionHandler.class)
class LicenseAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LicenseAdminService licenseAdminService;

    private License sampleLicense() {
        return new License(
                1L, "serial-001", "My License", "AS", "Server", "1.0", "Test Corp",
                "center", "true", "never", "test-user", 10, 8, 16384,
                0, 10, 0, 0, "<license/>", License.STATUS_ACTIVE, "FILE",
                Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("POST /api/v1/admin/licenses/import 导入成功返回 201")
    void shouldImportLicense() throws Exception {
        License license = sampleLicense();
        when(licenseAdminService.importLicense(any(LicenseImportRequest.class))).thenReturn(license);

        LicenseImportRequest request = new LicenseImportRequest("<license/>", "My License");

        mockMvc.perform(post("/api/v1/admin/licenses/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serial").value("serial-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/licenses/import 空文件返回 400")
    void shouldRejectBlankLicenseFile() throws Exception {
        LicenseImportRequest request = new LicenseImportRequest("   ", null);

        mockMvc.perform(post("/api/v1/admin/licenses/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(licenseAdminService, never()).importLicense(any());
    }

    @Test
    @DisplayName("GET /api/v1/admin/licenses 返回授权列表")
    void shouldListLicenses() throws Exception {
        LicenseSummary summary = LicenseSummary.from(sampleLicense());
        when(licenseAdminService.listLicenses()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/admin/licenses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serial").value("serial-001"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/licenses/{id} 返回授权详情")
    void shouldGetLicenseDetail() throws Exception {
        when(licenseAdminService.getLicense(1L)).thenReturn(sampleLicense());

        mockMvc.perform(get("/api/v1/admin/licenses/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serial").value("serial-001"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/licenses/{id} 授权不存在返回 404 LICENSE_001")
    void shouldReturn404WhenLicenseNotFound() throws Exception {
        when(licenseAdminService.getLicense(999L))
                .thenThrow(new LicenseException(ErrorCode.LICENSE_001, "授权不存在：999"));

        mockMvc.perform(get("/api/v1/admin/licenses/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LICENSE_001"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/licenses/{id}/verify 返回三层校验结果")
    void shouldVerifyLicense() throws Exception {
        LicenseVerifyResponse response = new LicenseVerifyResponse(
                "serial-001", true,
                List.of(new LicenseVerifyResponse.LayerResult("SIGNATURE", true, "ok")),
                "校验通过");
        when(licenseAdminService.verifyLicense(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/licenses/1/verify")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.serial").value("serial-001"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/licenses/{id} 删除成功返回 204")
    void shouldDeleteLicense() throws Exception {
        doNothing().when(licenseAdminService).deleteLicense(1L);

        mockMvc.perform(delete("/api/v1/admin/licenses/1"))
                .andExpect(status().isNoContent());

        verify(licenseAdminService, times(1)).deleteLicense(1L);
    }

    @Test
    @DisplayName("DELETE 存在在线实例返回 409 LICENSE_006")
    void shouldReturn409WhenOnlineInstances() throws Exception {
        doThrow(new LicenseException(ErrorCode.LICENSE_006, "存在在线实例，无法删除"))
                .when(licenseAdminService).deleteLicense(1L);

        mockMvc.perform(delete("/api/v1/admin/licenses/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LICENSE_006"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/licenses/{id}/disable 禁用成功返回 200")
    void shouldDisableLicense() throws Exception {
        License disabled = new License(
                1L, "serial-001", "My License", "AS", "Server", "1.0", "Test Corp",
                "center", "true", "never", "test-user", 10, 8, 16384,
                0, 10, 0, 0, "<license/>", License.STATUS_DISABLED, "FILE",
                Instant.now(), Instant.now());
        when(licenseAdminService.disableLicense(1L)).thenReturn(disabled);

        mockMvc.perform(put("/api/v1/admin/licenses/1/disable")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }
}