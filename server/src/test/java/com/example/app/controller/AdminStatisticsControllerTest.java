package com.example.app.controller;

import com.example.app.auth.JwtTokenService;
import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.model.StatisticsAlert;
import com.example.app.model.StatisticsDashboard;
import com.example.app.model.StatisticsOverview;
import com.example.app.model.StatisticsTrend;
import com.example.app.service.AuditService;
import com.example.app.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端统计控制器（wp-8）切片测试（TDD）。
 *
 * <p>覆盖 5 个端点：/statistics、/statistics/trend、/statistics/dashboard-v2、
 * /statistics/alerts、/statistics/export。</p>
 */
@WebMvcTest(AdminStatisticsController.class)
@Import(GlobalExceptionHandler.class)
class AdminStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUpAuth() {
        when(jwtTokenService.validateToken("valid-token")).thenReturn("admin");
    }

    private StatisticsOverview sampleOverview() {
        return new StatisticsOverview(3, 2, 1, 0, 5, 3, 2, 16, 32768, 8, 16384, 10);
    }

    @Test
    @DisplayName("GET /api/v1/admin/statistics 返回统计总览")
    void shouldReturnOverview() throws Exception {
        when(statisticsService.overview()).thenReturn(sampleOverview());

        mockMvc.perform(get("/api/v1/admin/statistics").header("Authorization", "Bearer valid-token").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenseCount").value(3))
                .andExpect(jsonPath("$.activeLicenseCount").value(2))
                .andExpect(jsonPath("$.onlineInstanceCount").value(3))
                .andExpect(jsonPath("$.auditLogCount").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/admin/statistics/trend 返回趋势序列")
    void shouldReturnTrend() throws Exception {
        StatisticsTrend trend = new StatisticsTrend(List.of(
                new StatisticsTrend.TrendPoint("2026-10-04", 1, 1),
                new StatisticsTrend.TrendPoint("2026-10-05", 2, 1)
        ));
        when(statisticsService.trend(7)).thenReturn(trend);

        mockMvc.perform(get("/api/v1/admin/statistics/trend").header("Authorization", "Bearer valid-token").param("days", "7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].date").value("2026-10-04"))
                .andExpect(jsonPath("$.days[0].instanceCount").value(1))
                .andExpect(jsonPath("$.days[1].licenseCount").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/statistics/dashboard-v2 返回仪表盘聚合")
    void shouldReturnDashboard() throws Exception {
        StatisticsDashboard dashboard = new StatisticsDashboard(
                sampleOverview(),
                new StatisticsTrend(List.of()),
                50.0,
                50.0,
                List.of(new StatisticsAlert("WARN", "INSTANCE_OFFLINE", "实例已下线：inst-1", "inst-1"))
        );
        when(statisticsService.dashboardV2()).thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/admin/statistics/dashboard-v2").header("Authorization", "Bearer valid-token").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.licenseCount").value(3))
                .andExpect(jsonPath("$.cpuUtilization").value(50.0))
                .andExpect(jsonPath("$.alerts[0].type").value("INSTANCE_OFFLINE"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/statistics/alerts 返回告警列表")
    void shouldReturnAlerts() throws Exception {
        when(statisticsService.alerts()).thenReturn(List.of(
                new StatisticsAlert("WARN", "LICENSE_EXPIRING", "授权即将过期", "S1")
        ));

        mockMvc.perform(get("/api/v1/admin/statistics/alerts").header("Authorization", "Bearer valid-token").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("LICENSE_EXPIRING"))
                .andExpect(jsonPath("$[0].level").value("WARN"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/statistics/export 返回 CSV 文本")
    void shouldExportCsv() throws Exception {
        when(statisticsService.export()).thenReturn("metric,value\nlicenseCount,3\n");

        mockMvc.perform(get("/api/v1/admin/statistics/export").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("metric,value\nlicenseCount,3\n"));
    }
}