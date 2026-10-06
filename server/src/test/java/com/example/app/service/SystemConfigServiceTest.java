package com.example.app.service;

import com.example.app.exception.ResourceNotFoundException;
import com.example.app.model.HeartbeatConfigRequest;
import com.example.app.model.SystemConfig;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigServiceImpl(systemConfigRepository);
    }

    @Test
    @DisplayName("getAllConfigs returns all system config items")
    void shouldReturnAllConfigs() {
        SystemConfig heartbeat = new SystemConfig(1L, "heartbeat.interval", "30", "心跳间隔（秒）", Instant.now());
        when(systemConfigRepository.findAll()).thenReturn(List.of(heartbeat));

        List<SystemConfig> result = systemConfigService.getAllConfigs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).configKey()).isEqualTo("heartbeat.interval");
        verify(systemConfigRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("updateHeartbeatConfig updates heartbeat interval and timeout multiplier")
    void shouldUpdateHeartbeatConfig() {
        SystemConfig interval = new SystemConfig(1L, "heartbeat.interval", "30", "心跳间隔（秒）", Instant.now());
        SystemConfig timeout = new SystemConfig(2L, "heartbeat.timeout_multiplier", "3", "心跳超时倍数", Instant.now());
        when(systemConfigRepository.findByKey("heartbeat.interval")).thenReturn(Optional.of(interval));
        when(systemConfigRepository.findByKey("heartbeat.timeout_multiplier")).thenReturn(Optional.of(timeout));
        when(systemConfigRepository.save(any(SystemConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<SystemConfig> updated = systemConfigService.updateHeartbeatConfig(
                new HeartbeatConfigRequest(60, 5));

        assertThat(updated).hasSize(2);
        assertThat(updated.get(0).configValue()).isEqualTo("60");
        assertThat(updated.get(1).configValue()).isEqualTo("5");

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(SystemConfig::configValue)
                .containsExactlyInAnyOrder("60", "5");
    }

    @Test
    @DisplayName("updateHeartbeatConfig throws when heartbeat interval config missing")
    void shouldThrowWhenHeartbeatIntervalMissing() {
        when(systemConfigRepository.findByKey("heartbeat.interval")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemConfigService.updateHeartbeatConfig(
                new HeartbeatConfigRequest(60, 5)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("heartbeat.interval");

        verify(systemConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("reloadConfig returns reloaded config list")
    void shouldReloadConfig() {
        SystemConfig heartbeat = new SystemConfig(1L, "heartbeat.interval", "30", "心跳间隔（秒）", Instant.now());
        when(systemConfigRepository.findAll()).thenReturn(List.of(heartbeat));

        List<SystemConfig> result = systemConfigService.reloadConfig();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).configKey()).isEqualTo("heartbeat.interval");
        verify(systemConfigRepository, times(1)).findAll();
    }
}