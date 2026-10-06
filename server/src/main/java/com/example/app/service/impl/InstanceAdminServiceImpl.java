package com.example.app.service.impl;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.model.HistoryInstance;
import com.example.app.model.Instance;
import com.example.app.repository.HistoryInstanceRepository;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.SystemConfigRepository;
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

    public InstanceAdminServiceImpl(InstanceRepository instanceRepository,
                                    HistoryInstanceRepository historyInstanceRepository,
                                    SystemConfigRepository systemConfigRepository) {
        this.instanceRepository = instanceRepository;
        this.historyInstanceRepository = historyInstanceRepository;
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    public List<Instance> listOnlineInstances() {
        return instanceRepository.findByStatus(Instance.STATUS_ONLINE);
    }

    @Override
    public List<Instance> listOfflineInstances() {
        return instanceRepository.findByStatus(Instance.STATUS_OFFLINE);
    }

    @Override
    public List<HistoryInstance> listHistoryInstances() {
        return historyInstanceRepository.findAll();
    }

    @Override
    public Instance getInstance(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new LicenseException(ErrorCode.INSTANCE_002, "实例不存在：" + id));
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