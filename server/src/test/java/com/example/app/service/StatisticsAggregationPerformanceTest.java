package com.example.app.service;

import com.example.app.model.Instance;
import com.example.app.model.License;
import com.example.app.repository.AuditLogRepository;
import com.example.app.repository.HistoryInstanceRepository;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.LicenseRepository;
import com.example.app.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 聚合查询性能回归测试（wp-8 聚合查询性能回归）。
 *
 * <p>验证统计聚合（overview / dashboard / alerts）在较大数据量下仍保持单次遍历
 * （single-pass）线性复杂度，避免 N+1 查询导致的性能退化。通过时间预算断言
 * 捕获未来引入的 O(N²) 或逐条查询回归。</p>
 */
class StatisticsAggregationPerformanceTest {

    private static final int LICENSE_COUNT = 2000;
    private static final int INSTANCE_COUNT = 5000;
    private static final long TIME_BUDGET_MS = 2000;

    private LicenseRepository licenseRepository;
    private InstanceRepository instanceRepository;
    private HistoryInstanceRepository historyInstanceRepository;
    private AuditLogRepository auditLogRepository;
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        licenseRepository = new LicenseRepository();
        instanceRepository = new InstanceRepository();
        historyInstanceRepository = new HistoryInstanceRepository();
        auditLogRepository = new AuditLogRepository();
        statisticsService = new StatisticsServiceImpl(
                licenseRepository, instanceRepository, historyInstanceRepository, auditLogRepository);
    }

    private void seedLargeDataset() {
        Instant now = Instant.now();
        for (int i = 0; i < LICENSE_COUNT; i++) {
            licenseRepository.save(new License(
                    null, "SER-" + i, "License-" + i, "InforSuiteAS", "AS", "9.0",
                    "Acme", "center", "true", "never", "user",
                    10, 8, 16384, i % 5, 10 - (i % 5), i % 8, (i % 5) * 1024,
                    null, License.STATUS_ACTIVE, "FILE", now, now));
        }
        for (int i = 0; i < INSTANCE_COUNT; i++) {
            String status = (i % 3 == 0) ? Instance.STATUS_OFFLINE : Instance.STATUS_ONLINE;
            instanceRepository.save(new Instance(
                    null, "inst-" + i, 1L, "uuid-" + i, "InforSuiteAS", "AS", "9.0", "Standard",
                    "host-" + i, "10.0.0.1", "AA:BB:CC:DD:EE:FF", "VM", 4, 8192, null,
                    status, now, now, status.equals(Instance.STATUS_OFFLINE) ? now : null, now, now));
        }
    }

    @Test
    @DisplayName("overview 在 2000 授权 + 5000 实例下线性聚合且不超时")
    void shouldAggregateOverviewWithinBudget() {
        seedLargeDataset();

        long start = System.nanoTime();
        var overview = statisticsService.overview();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(overview.licenseCount()).isEqualTo(LICENSE_COUNT);
        assertThat(overview.instanceCount()).isEqualTo(INSTANCE_COUNT);
        assertThat(elapsedMs).isLessThan(TIME_BUDGET_MS);
    }

    @Test
    @DisplayName("dashboardV2 聚合总览+趋势+告警在预算内完成")
    void shouldBuildDashboardWithinBudget() {
        seedLargeDataset();

        long start = System.nanoTime();
        var dashboard = statisticsService.dashboardV2();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(dashboard.overview().licenseCount()).isEqualTo(LICENSE_COUNT);
        assertThat(dashboard.overview().instanceCount()).isEqualTo(INSTANCE_COUNT);
        assertThat(dashboard.alerts()).isNotEmpty();
        assertThat(elapsedMs).isLessThan(TIME_BUDGET_MS);
    }

    @Test
    @DisplayName("alerts 在大数据集下线性扫描且不超时")
    void shouldScanAlertsWithinBudget() {
        seedLargeDataset();

        long start = System.nanoTime();
        var alerts = statisticsService.alerts();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(alerts).isNotEmpty();
        assertThat(elapsedMs).isLessThan(TIME_BUDGET_MS);
    }
}