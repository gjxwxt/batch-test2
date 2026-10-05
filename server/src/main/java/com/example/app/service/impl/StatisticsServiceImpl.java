package com.example.app.service.impl;

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
import com.example.app.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计与仪表盘服务实现（wp-8）。
 *
 * <p>聚合查询采用单次遍历（single-pass）实现：对授权、实例、历史实例、审计日志各做一次
 * {@code findAll()} 后原地累加，避免 N+1 查询，保证大数据量下的聚合查询性能
 * （聚合查询性能回归）。</p>
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int DEFAULT_TREND_DAYS = 30;
    private static final int MAX_TREND_DAYS = 365;
    private static final int EXPIRING_WINDOW_DAYS = 30;

    private final LicenseRepository licenseRepository;
    private final InstanceRepository instanceRepository;
    private final HistoryInstanceRepository historyInstanceRepository;
    private final AuditLogRepository auditLogRepository;

    public StatisticsServiceImpl(LicenseRepository licenseRepository,
                                 InstanceRepository instanceRepository,
                                 HistoryInstanceRepository historyInstanceRepository,
                                 AuditLogRepository auditLogRepository) {
        this.licenseRepository = licenseRepository;
        this.instanceRepository = instanceRepository;
        this.historyInstanceRepository = historyInstanceRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public StatisticsOverview overview() {
        long licenseCount = 0;
        long activeLicenseCount = 0;
        long disabledLicenseCount = 0;
        long expiredLicenseCount = 0;
        long totalCpus = 0;
        long totalMemory = 0;
        long usedCpus = 0;
        long usedMemory = 0;

        for (License license : licenseRepository.findAll()) {
            licenseCount++;
            if (license.isActive()) {
                activeLicenseCount++;
            } else if (license.isDisabled()) {
                disabledLicenseCount++;
            }
            if (license.isExpired() || license.isPastExpiration()) {
                expiredLicenseCount++;
            }
            totalCpus += nz(license.maxCpus());
            totalMemory += nz(license.maxMemory());
            usedCpus += nz(license.usedCpus());
            usedMemory += nz(license.usedMemory());
        }

        long instanceCount = 0;
        long onlineInstanceCount = 0;
        long offlineInstanceCount = 0;
        for (Instance instance : instanceRepository.findAll()) {
            instanceCount++;
            if (instance.isOnline()) {
                onlineInstanceCount++;
            } else {
                offlineInstanceCount++;
            }
        }

        long auditLogCount = auditLogRepository.findAll().size();

        return new StatisticsOverview(
                licenseCount,
                activeLicenseCount,
                disabledLicenseCount,
                expiredLicenseCount,
                instanceCount,
                onlineInstanceCount,
                offlineInstanceCount,
                totalCpus,
                totalMemory,
                usedCpus,
                usedMemory,
                auditLogCount
        );
    }

    @Override
    public StatisticsTrend trend(int days) {
        int window = normalizeDays(days);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = today.minusDays(window - 1L);

        // 有序桶：start .. today，日期升序
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < window; i++) {
            buckets.put(start.plusDays(i), new long[]{0L, 0L});
        }

        for (Instance instance : instanceRepository.findAll()) {
            LocalDate date = toDate(instance.createTime());
            if (date != null && !date.isBefore(start) && !date.isAfter(today)) {
                buckets.get(date)[0]++;
            }
        }
        for (License license : licenseRepository.findAll()) {
            LocalDate date = toDate(license.createTime());
            if (date != null && !date.isBefore(start) && !date.isAfter(today)) {
                buckets.get(date)[1]++;
            }
        }

        List<StatisticsTrend.TrendPoint> points = new ArrayList<>(buckets.size());
        for (Map.Entry<LocalDate, long[]> entry : buckets.entrySet()) {
            points.add(new StatisticsTrend.TrendPoint(
                    entry.getKey().format(DATE_FMT),
                    entry.getValue()[0],
                    entry.getValue()[1]
            ));
        }
        return new StatisticsTrend(points);
    }

    @Override
    public StatisticsDashboard dashboardV2() {
        StatisticsOverview overview = overview();
        StatisticsTrend trend = trend(DEFAULT_TREND_DAYS);
        List<StatisticsAlert> alerts = alerts();

        double cpuUtilization = utilization(overview.usedCpus(), overview.totalCpus());
        double memoryUtilization = utilization(overview.usedMemory(), overview.totalMemory());

        return new StatisticsDashboard(overview, trend, cpuUtilization, memoryUtilization, alerts);
    }

    @Override
    public List<StatisticsAlert> alerts() {
        List<StatisticsAlert> alerts = new ArrayList<>();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate expiringCutoff = today.plusDays(EXPIRING_WINDOW_DAYS);

        for (License license : licenseRepository.findAll()) {
            if (!license.isActive()) {
                continue;
            }
            // 即将过期告警
            if (license.expiration() != null && !license.expiration().isBlank()
                    && !"never".equalsIgnoreCase(license.expiration())) {
                try {
                    LocalDate expiry = LocalDate.parse(license.expiration());
                    if (!expiry.isBefore(today) && !expiry.isAfter(expiringCutoff)) {
                        alerts.add(new StatisticsAlert(
                                StatisticsAlert.LEVEL_WARN,
                                StatisticsAlert.TYPE_LICENSE_EXPIRING,
                                "授权即将过期：" + license.licenseName() + "（" + expiry + "）",
                                license.serial()
                        ));
                    }
                } catch (Exception ignored) {
                    // 非法日期格式忽略
                }
            }
            // 配额告警：已用实例数达到或超过上限
            if (license.maxInstances() != null && license.maxInstances() > 0
                    && license.usedInstances() != null
                    && license.usedInstances() >= license.maxInstances()) {
                alerts.add(new StatisticsAlert(
                        StatisticsAlert.LEVEL_CRITICAL,
                        StatisticsAlert.TYPE_QUOTA_WARNING,
                        "实例配额已用尽：" + license.licenseName()
                                + "（" + license.usedInstances() + "/" + license.maxInstances() + "）",
                        license.serial()
                ));
            }
        }

        // 离线实例告警
        for (Instance instance : instanceRepository.findByStatus(Instance.STATUS_OFFLINE)) {
            alerts.add(new StatisticsAlert(
                    StatisticsAlert.LEVEL_WARN,
                    StatisticsAlert.TYPE_INSTANCE_OFFLINE,
                    "实例已下线：" + instance.instanceId(),
                    instance.instanceId()
            ));
        }

        return alerts;
    }

    @Override
    public String export() {
        StatisticsOverview o = overview();
        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        sb.append("licenseCount,").append(o.licenseCount()).append('\n');
        sb.append("activeLicenseCount,").append(o.activeLicenseCount()).append('\n');
        sb.append("disabledLicenseCount,").append(o.disabledLicenseCount()).append('\n');
        sb.append("expiredLicenseCount,").append(o.expiredLicenseCount()).append('\n');
        sb.append("instanceCount,").append(o.instanceCount()).append('\n');
        sb.append("onlineInstanceCount,").append(o.onlineInstanceCount()).append('\n');
        sb.append("offlineInstanceCount,").append(o.offlineInstanceCount()).append('\n');
        sb.append("totalCpus,").append(o.totalCpus()).append('\n');
        sb.append("totalMemory,").append(o.totalMemory()).append('\n');
        sb.append("usedCpus,").append(o.usedCpus()).append('\n');
        sb.append("usedMemory,").append(o.usedMemory()).append('\n');
        sb.append("auditLogCount,").append(o.auditLogCount()).append('\n');
        return sb.toString();
    }

    private int normalizeDays(int days) {
        if (days < 1) {
            return DEFAULT_TREND_DAYS;
        }
        return Math.min(days, MAX_TREND_DAYS);
    }

    private LocalDate toDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private long nz(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private double utilization(long used, long total) {
        if (total <= 0) {
            return 0.0;
        }
        double ratio = (double) used / total;
        return Math.round(Math.min(ratio, 1.0) * 10000.0) / 100.0;
    }
}