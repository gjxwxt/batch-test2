package com.example.app.service.impl;

import com.example.app.exception.ResourceNotFoundException;
import com.example.app.model.HeartbeatConfigRequest;
import com.example.app.model.SystemConfig;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String HEARTBEAT_INTERVAL_KEY = "heartbeat.interval";
    private static final String HEARTBEAT_TIMEOUT_KEY = "heartbeat.timeout_multiplier";

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigServiceImpl(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
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
        return List.of(updatedInterval, updatedTimeout);
    }

    @Override
    public List<SystemConfig> reloadConfig() {
        return systemConfigRepository.findAll();
    }
}