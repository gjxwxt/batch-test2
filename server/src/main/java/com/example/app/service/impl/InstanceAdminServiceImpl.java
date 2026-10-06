package com.example.app.service.impl;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.model.AuditOperationType;
import com.example.app.model.HistoryInstance;
import com.example.app.model.Instance;
import com.example.app.repository.HistoryInstanceRepository;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.AuditService;
import com.example.app.service.InstanceAdminService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 实例生命周期与监控服务实现（wp-5）。
 *
 * <p>超时/归档/历史清理阈值均取自 {@link SystemConfigRepository} 预置配置
 * （心跳间隔、超时倍数、归档天数、历史删除天数），与 V1__init_schema.sql 种子一致。</p>
 */
@Service
public class InstanceAdminServiceImpl implements InstanceAdminService {

    private final InstanceRepository instanceRepository;
    private final HistoryInstanceRepository historyInstanceRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AuditService auditService;

    public InstanceAdminServiceImpl(InstanceRepository instanceRepository,
                                    HistoryInstanceRepository historyInstanceRepository,
                                    SystemConfigRepository systemConfigRepository,
                                    AuditService auditService) {
        this.instanceRepository = instanceRepository;
        this.historyInstanceRepository = historyInstanceRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.auditService = auditService;
    }

    @Override
    public List<Instance> listOnlineInstances() {
        List<Instance> result = instanceRepository.findByStatus(Instance.STATUS_ONLINE);
        auditService.record(AuditOperationType.INSTANCE_QUERY, "admin", null,
                null, "SUCCESS", "查询在线实例，共 " + result.size() + " 条");
        return result;
    }

    @Override
    public List<Instance> listOfflineInstances() {
        List<Instance> result = instanceRepository.findByStatus(Instance.STATUS_OFFLINE);
        auditService.record(AuditOperationType.INSTANCE_QUERY, "admin", null,
                null, "SUCCESS", "查询下线实例，共 " + result.size() + " 条");
        return result;
    }

    @Override
    public List<HistoryInstance> listHistoryInstances() {
        List<HistoryInstance> result = historyInstanceRepository.findAll();
        auditService.record(AuditOperationType.INSTANCE_QUERY, "admin", null,
                null, "SUCCESS", "查询历史实例，共 " + result.size() + " 条");
        return result;
    }

    @Override
    public Instance getInstance(Long id) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new LicenseException(ErrorCode.INSTANCE_002, "实例不存在：" + id));
        auditService.record(AuditOperationType.INSTANCE_QUERY, "admin", null,
                instance.instanceId(), "SUCCESS", "查看实例详情：" + instance.instanceId());
        return instance;
    }

    @Override
    public int detectTimeouts() {
        int interval = parseIntConfig(SystemConfigRepository.KEY_HEARTBEAT_INTERVAL, 30);
        int timeoutCount = parseIntConfig(SystemConfigRepository.KEY_HEARTBEAT_TIMEOUT_COUNT, 3);
        long timeoutSeconds = (long) interval * timeoutCount;

        Instant now = Instant.now();
        int marked = 0;
        for (Instance instance : instanceRepository.findByStatus(Instance.STATUS_ONLINE)) {
            Instant lastHeartbeat = instance.lastHeartbeatTime();
            if (lastHeartbeat == null) {
                lastHeartbeat = instance.onlineTime();
            }
            if (lastHeartbeat == null) {
                continue;
            }
            if (Duration.between(lastHeartbeat, now).getSeconds() > timeoutSeconds) {
                Instance offline = new Instance(
                        instance.id(),
                        instance.instanceId(),
                        instance.licenseId(),
                        instance.clientUuid(),
                        instance.proname(),
                        instance.productType(),
                        instance.productVersion(),
                        instance.productSpec(),
                        instance.hostname(),
                        instance.ipAddress(),
                        instance.mac(),
                        instance.machineType(),
                        instance.currentCpus(),
                        instance.currentMemory(),
                        instance.extendedAttributes(),
                        Instance.STATUS_OFFLINE,
                        instance.onlineTime(),
                        instance.lastHeartbeatTime(),
                        now,
                        instance.createTime(),
                        now
                );
                instanceRepository.save(offline);
                auditService.record(AuditOperationType.TIMEOUT, "system", null,
                        instance.instanceId(), "SUCCESS", "心跳超时，实例下线：" + instance.instanceId());
                marked++;
            }
        }
        return marked;
    }

    @Override
    public int archiveOfflineInstances() {
        int archiveDays = parseIntConfig(SystemConfigRepository.KEY_ARCHIVE_AFTER_DAYS, 30);
        Instant now = Instant.now();
        int archived = 0;
        for (Instance instance : instanceRepository.findByStatus(Instance.STATUS_OFFLINE)) {
            Instant offlineTime = instance.offlineTime();
            if (offlineTime == null) {
                continue;
            }
            if (Duration.between(offlineTime, now).toDays() >= archiveDays) {
                historyInstanceRepository.save(HistoryInstance.from(instance, now));
                instanceRepository.deleteById(instance.id());
                auditService.record(AuditOperationType.ARCHIVE, "system", null,
                        instance.instanceId(), "SUCCESS", "实例归档：" + instance.instanceId());
                archived++;
            }
        }
        return archived;
    }

    @Override
    public int deleteExpiredHistory() {
        int deleteDays = parseIntConfig(SystemConfigRepository.KEY_HISTORY_DELETE_AFTER_DAYS, 90);
        Instant now = Instant.now();
        int deleted = 0;
        for (HistoryInstance history : historyInstanceRepository.findAll()) {
            Instant archivedTime = history.archivedTime();
            if (archivedTime == null) {
                continue;
            }
            if (Duration.between(archivedTime, now).toDays() >= deleteDays) {
                historyInstanceRepository.deleteById(history.id());
                auditService.record(AuditOperationType.INSTANCE_DELETE, "system", null,
                        history.instanceId(), "SUCCESS", "历史实例删除：" + history.instanceId());
                deleted++;
            }
        }
        return deleted;
    }

    private int parseIntConfig(String key, int defaultValue) {
        String value = systemConfigRepository.getValueOrDefault(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}