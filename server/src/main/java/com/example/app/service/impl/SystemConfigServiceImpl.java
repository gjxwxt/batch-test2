package com.example.app.service.impl;

import com.example.app.exception.ResourceNotFoundException;
import com.example.app.model.AuditOperationType;
import com.example.app.model.HeartbeatConfigRequest;
import com.example.app.model.SystemConfig;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.AuditService;
import com.example.app.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String HEARTBEAT_INTERVAL_KEY = "heartbeat.interval";
    private static final String HEARTBEAT_TIMEOUT_KEY = SystemConfigRepository.KEY_HEARTBEAT_TIMEOUT_COUNT;

    private final SystemConfigRepository systemConfigRepository;
    private final AuditService auditService;

    public SystemConfigServiceImpl(SystemConfigRepository systemConfigRepository,
                                   AuditService auditService) {
        this.systemConfigRepository = systemConfigRepository;
        this.auditService = auditService;
    }

    @Override
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Override
    public List<SystemConfig> updateHeartbeatConfig(HeartbeatConfigRequest request) {
        SystemConfig interval = systemConfigRepository.findByKey(HEARTBEAT_INTERVAL_KEY)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfig", HEARTBEAT_INTERVAL_KEY));
        SystemConfig timeout = systemConfigRepository.findByKey(HEARTBEAT_TIMEOUT_KEY)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfig", HEARTBEAT_TIMEOUT_KEY));

        Instant now = Instant.now();
        SystemConfig updatedInterval = new SystemConfig(
                interval.id(), interval.configKey(),
                String.valueOf(request.heartbeatInterval()), interval.configDesc(), now);
        SystemConfig updatedTimeout = new SystemConfig(
                timeout.id(), timeout.configKey(),
                String.valueOf(request.timeoutMultiplier()), timeout.configDesc(), now);

        systemConfigRepository.save(updatedInterval);
        systemConfigRepository.save(updatedTimeout);
        auditService.record(AuditOperationType.CONFIG_UPDATE, "admin", null,
                HEARTBEAT_INTERVAL_KEY, "SUCCESS",
                "更新心跳配置：interval=" + request.heartbeatInterval() + ", timeout=" + request.timeoutMultiplier());
        return List.of(updatedInterval, updatedTimeout);
    }

    @Override
    public List<SystemConfig> reloadConfig() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        auditService.record(AuditOperationType.CONFIG_RELOAD, "admin", null,
                null, "SUCCESS", "重载系统配置，共 " + configs.size() + " 项");
        return configs;
    }
}