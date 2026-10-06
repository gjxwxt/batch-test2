package com.example.app.service;

import com.example.app.model.HeartbeatConfigRequest;
import com.example.app.model.SystemConfig;

import java.util.List;

public interface SystemConfigService {

    List<SystemConfig> getAllConfigs();

    List<SystemConfig> updateHeartbeatConfig(HeartbeatConfigRequest request);

    List<SystemConfig> reloadConfig();
}