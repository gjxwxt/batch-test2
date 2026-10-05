package com.example.app.service;

import com.example.app.model.AuditLog;
import com.example.app.model.AuditOperationType;
import com.example.app.model.Instance;
import com.example.app.model.License;
import com.example.app.model.StatisticsAlert;
import com.example.app.model.StatisticsDashboard;
import com.example.app.model.StatisticsOverview;
import com.example.app.model.StatisticsTrend;
import com.example.app.repository.AuditLogRepository;
import com.example.app.repository.HistoryInstanceRepository;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.LicenseRepository;
import com.example.app.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统计与仪表盘服务（wp-8）单元测试（TDD）。
 *
 * <p>使用真实内存仓储注入数据，验证聚合查询（overview / trend / dashboard / alerts / export）
 * 的计数与计算正确性。</p>
 */
class StatisticsServiceTest {

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

    private License license(Long id, String serial, String status, String expiration,
                            Integer maxInstances, Integer usedInstances,
                            Integer maxCpus, Integer usedCpus,
                            Integer maxMemory, Integer usedMemory,
                            Instant createTime) {
        return new License(
                id, serial, "License-" + serial, "InforSuiteAS", "AS", "9.0",
                "Acme", "center", "true", expiration, "user",
                maxInstances, maxCpus, maxMemory, usedInstances,
                maxInstances != null && usedInstances != null ? maxInstances - usedInstances : 0,
                usedCpus, usedMemory, null, status, "FILE", createTime, createTime);
    }

    private Instance instance(Long id, String instanceId, String status, Instant createTime) {
        return new Instance(
                id, instanceId, 1L, "uuid-" + instanceId, "InforSuiteAS", "AS", "9.0", "Standard",
                "host-" + instanceId, "10.0.0.1", "AA:BB:CC:DD:EE:FF", "VM", 4, 8192, null,
                status, createTime, createTime,
                status.equals(Instance.STATUS_OFFLINE) ? createTime : null, createTime, createTime);
    }

    @Test
    @DisplayName("overview 聚合授权/实例/审计指标")
    void shouldAggregateOverview() {
        Instant now = Instant.now();
        licenseRepository.save(license(1L, "S1", License.STATUS_ACTIVE, "never", 10, 2, 8, 4, 16384, 8192, now));
        licenseRepository.save(license(2L, "S2", License.STATUS_DISABLED, "never", 5, 0, 4, 0, 8192, 0, now));
        licenseRepository.save(license(3L, "S3", License.STATUS_ACTIVE, "2020-01-01", 5, 5, 4, 4, 8192, 8192, now));

        instanceRepository.save(instance(1L, "inst-1", Instance.STATUS_ONLINE, now));
        instanceRepository.save(instance(2L, "inst-2", Instance.STATUS_OFFLINE, now));

        auditLogRepository.save(new AuditLog("log-1", AuditOperationType.LOGIN, "登录", "admin",
                "127.0.0.1", null, "SUCCESS", null, now));

        StatisticsOverview o = statisticsService.overview();

        assertThat(o.licenseCount()).isEqualTo(3);
        assertThat(o.activeLicenseCount()).isEqualTo(2);
        assertThat(o.disabledLicenseCount()).isEqualTo(1);
        assertThat(o.expiredLicenseCount()).isEqualTo(1);
        assertThat(o.instanceCount()).isEqualTo(2);
        assertThat(o.onlineInstanceCount()).isEqualTo(1);
        assertThat(o.offlineInstanceCount()).isEqualTo(1);
        assertThat(o.totalCpus()).isEqualTo(16);
        assertThat(o.totalMemory()).isEqualTo(32768);
        assertThat(o.usedCpus()).isEqualTo(8);
        assertThat(o.usedMemory()).isEqualTo(16384);
        assertThat(o.auditLogCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("overview 空数据返回全零")
    void shouldReturnZeroWhenEmpty() {
        StatisticsOverview o = statisticsService.overview();

        assertThat(o.licenseCount()).isZero();
        assertThat(o.instanceCount()).isZero();
        assertThat(o.auditLogCount()).isZero();
        assertThat(o.totalCpus()).isZero();
        assertThat(o.usedMemory()).isZero();
    }

    @Test
    @DisplayName("trend 按天聚合实例注册与授权导入")
    void shouldAggregateTrendByDay() {
        Instant today = Instant.now();
        Instant yesterday = today.minusSeconds(86400);

        licenseRepository.save(license(1L, "S1", License.STATUS_ACTIVE, "never", 10, 0, 8, 0, 16384, 0, yesterday));
        licenseRepository.save(license(2L, "S2", License.STATUS_ACTIVE, "never", 10, 0, 8, 0, 16384, 0, today));

        instanceRepository.save(instance(1L, "inst-1", Instance.STATUS_ONLINE, yesterday));
        instanceRepository.save(instance(2L, "inst-2", Instance.STATUS_ONLINE, today));
        instanceRepository.save(instance(3L, "inst-3", Instance.STATUS_ONLINE, today));

        StatisticsTrend trend = statisticsService.trend(7);

        assertThat(trend.days()).hasSize(7);
        // 最后一天（今天）应含 2 个实例、1 个授权
        StatisticsTrend.TrendPoint last = trend.days().get(trend.days().size() - 1);
        assertThat(last.date()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
        assertThat(last.instanceCount()).isEqualTo(2);
        assertThat(last.licenseCount()).isEqualTo(1);
        // 倒数第二天（昨天）应含 1 个实例、1 个授权
        StatisticsTrend.TrendPoint prev = trend.days().get(trend.days().size() - 2);
        assertThat(prev.instanceCount()).isEqualTo(1);
        assertThat(prev.licenseCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("trend 非法天数回退默认窗口")
    void shouldFallbackToDefaultWindowForInvalidDays() {
        StatisticsTrend trend = statisticsService.trend(0);
        assertThat(trend.days()).hasSize(30);
    }

    @Test
    @DisplayName("dashboardV2 聚合总览/趋势/利用率/告警")
    void shouldBuildDashboard() {
        Instant now = Instant.now();
        licenseRepository.save(license(1L, "S1", License.STATUS_ACTIVE, "never", 10, 5, 8, 4, 16384, 8192, now));
        instanceRepository.save(instance(1L, "inst-1", Instance.STATUS_ONLINE, now));

        StatisticsDashboard d = statisticsService.dashboardV2();

        assertThat(d.overview().licenseCount()).isEqualTo(1);
        assertThat(d.trend().days()).isNotEmpty();
        assertThat(d.cpuUtilization()).isEqualTo(50.0);
        assertThat(d.memoryUtilization()).isEqualTo(50.0);
        assertThat(d.alerts()).isNotNull();
    }

    @Test
    @DisplayName("alerts 识别即将过期授权、配额用尽与离线实例")
    void shouldDetectAlerts() {
        Instant now = Instant.now();
        String expiring = LocalDate.now(ZoneOffset.UTC).plusDays(10).toString();
        licenseRepository.save(license(1L, "S1", License.STATUS_ACTIVE, expiring, 10, 10, 8, 8, 16384, 16384, now));
        licenseRepository.save(license(2L, "S2", License.STATUS_ACTIVE, "never", 10, 1, 8, 1, 16384, 1024, now));
        instanceRepository.save(instance(1L, "inst-1", Instance.STATUS_OFFLINE, now));

        List<StatisticsAlert> alerts = statisticsService.alerts();

        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(a -> StatisticsAlert.TYPE_LICENSE_EXPIRING.equals(a.type()));
        assertThat(alerts).anyMatch(a -> StatisticsAlert.TYPE_QUOTA_WARNING.equals(a.type()));
        assertThat(alerts).anyMatch(a -> StatisticsAlert.TYPE_INSTANCE_OFFLINE.equals(a.type()));
    }

    @Test
    @DisplayName("export 输出 CSV 统计文本")
    void shouldExportCsv() {
        Instant now = Instant.now();
        licenseRepository.save(license(1L, "S1", License.STATUS_ACTIVE, "never", 10, 2, 8, 4, 16384, 8192, now));

        String csv = statisticsService.export();

        assertThat(csv).contains("metric,value");
        assertThat(csv).contains("licenseCount,1");
        assertThat(csv).contains("activeLicenseCount,1");
        assertThat(csv).contains("usedCpus,4");
    }
}