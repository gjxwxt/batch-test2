package com.example.app.service;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.model.HistoryInstance;
import com.example.app.model.Instance;
import com.example.app.repository.HistoryInstanceRepository;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.impl.InstanceAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 实例生命周期与监控服务（wp-5）单元测试（TDD）。
 *
 * <p>覆盖 req-18 超时下线 / req-19 归档 / req-20 历史删除 / req-21 在线查询 /
 * req-22 下线查询 / req-23 详情查看，以及历史实例查询。</p>
 */
class InstanceAdminServiceTest {

    private InstanceRepository instanceRepository;
    private HistoryInstanceRepository historyInstanceRepository;
    private SystemConfigRepository systemConfigRepository;
    private InstanceAdminService service;

    @BeforeEach
    void setUp() {
        instanceRepository = new InstanceRepository();
        historyInstanceRepository = new HistoryInstanceRepository();
        systemConfigRepository = new SystemConfigRepository();
        service = new InstanceAdminServiceImpl(instanceRepository, historyInstanceRepository, systemConfigRepository);
    }

    private Instance onlineInstance(String instanceId, Instant lastHeartbeat) {
        return new Instance(
                null, instanceId, 1L, "uuid-" + instanceId, "InforSuiteAS", "AS",
                "9.0", "Standard", "host-" + instanceId, "10.0.0.1", "AA:BB:CC:DD:EE:FF",
                "VM", 4, 8192, null, Instance.STATUS_ONLINE,
                lastHeartbeat, lastHeartbeat, null, lastHeartbeat, lastHeartbeat
        );
    }

    private Instance offlineInstance(String instanceId, Instant offlineTime) {
        return new Instance(
                null, instanceId, 1L, "uuid-" + instanceId, "InforSuiteAS", "AS",
                "9.0", "Standard", "host-" + instanceId, "10.0.0.1", "AA:BB:CC:DD:EE:FF",
                "VM", 4, 8192, null, Instance.STATUS_OFFLINE,
                offlineTime.minus(1, ChronoUnit.HOURS), offlineTime.minus(1, ChronoUnit.HOURS),
                offlineTime, offlineTime.minus(1, ChronoUnit.HOURS), offlineTime
        );
    }

    @Test
    @DisplayName("req-18 心跳超时：最近心跳超过阈值(30s×3=90s)的在线实例被标记下线")
    void shouldMarkTimedOutInstanceOffline() {
        Instant now = Instant.now();
        // 心跳在 5 分钟前 → 超过 90s 阈值
        instanceRepository.save(onlineInstance("inst-timeout", now.minus(5, ChronoUnit.MINUTES)));
        // 心跳在 10 秒前 → 未超时
        instanceRepository.save(onlineInstance("inst-fresh", now.minus(10, ChronoUnit.SECONDS)));

        int marked = service.detectTimeouts();

        assertThat(marked).isEqualTo(1);
        List<Instance> online = service.listOnlineInstances();
        List<Instance> offline = service.listOfflineInstances();
        assertThat(online).extracting(Instance::instanceId).containsExactly("inst-fresh");
        assertThat(offline).extracting(Instance::instanceId).containsExactly("inst-timeout");
        assertThat(offline.get(0).offlineTime()).isNotNull();
    }

    @Test
    @DisplayName("req-18 心跳超时：无超时实例时返回 0")
    void shouldReturnZeroWhenNoTimeout() {
        Instant now = Instant.now();
        instanceRepository.save(onlineInstance("inst-fresh", now.minus(10, ChronoUnit.SECONDS)));

        int marked = service.detectTimeouts();

        assertThat(marked).isZero();
        assertThat(service.listOnlineInstances()).hasSize(1);
    }

    @Test
    @DisplayName("req-19 实例归档：下线超过 30 天的实例迁移到历史表并从在线表移除")
    void shouldArchiveOfflineInstanceAfterDays() {
        Instant now = Instant.now();
        // 下线 40 天前 → 应归档
        instanceRepository.save(offlineInstance("inst-old", now.minus(40, ChronoUnit.DAYS)));
        // 下线 5 天前 → 不应归档
        instanceRepository.save(offlineInstance("inst-recent", now.minus(5, ChronoUnit.DAYS)));

        int archived = service.archiveOfflineInstances();

        assertThat(archived).isEqualTo(1);
        assertThat(service.listOfflineInstances()).extracting(Instance::instanceId)
                .containsExactly("inst-recent");
        assertThat(service.listHistoryInstances()).extracting(HistoryInstance::instanceId)
                .containsExactly("inst-old");
        assertThat(service.listHistoryInstances().get(0).archivedTime()).isNotNull();
    }

    @Test
    @DisplayName("req-20 历史删除：归档超过 90 天的历史实例被永久删除")
    void shouldDeleteExpiredHistory() {
        Instant now = Instant.now();
        historyInstanceRepository.save(HistoryInstance.from(
                offlineInstance("hist-old", now.minus(100, ChronoUnit.DAYS)),
                now.minus(100, ChronoUnit.DAYS)));
        historyInstanceRepository.save(HistoryInstance.from(
                offlineInstance("hist-recent", now.minus(10, ChronoUnit.DAYS)),
                now.minus(10, ChronoUnit.DAYS)));

        int deleted = service.deleteExpiredHistory();

        assertThat(deleted).isEqualTo(1);
        assertThat(service.listHistoryInstances()).extracting(HistoryInstance::instanceId)
                .containsExactly("hist-recent");
    }

    @Test
    @DisplayName("req-21 在线实例查询：仅返回 ONLINE 实例")
    void shouldListOnlyOnlineInstances() {
        Instant now = Instant.now();
        instanceRepository.save(onlineInstance("inst-online", now));
        instanceRepository.save(offlineInstance("inst-offline", now.minus(1, ChronoUnit.HOURS)));

        List<Instance> online = service.listOnlineInstances();

        assertThat(online).extracting(Instance::instanceId).containsExactly("inst-online");
    }

    @Test
    @DisplayName("req-22 下线实例查询：仅返回 OFFLINE 实例")
    void shouldListOnlyOfflineInstances() {
        Instant now = Instant.now();
        instanceRepository.save(onlineInstance("inst-online", now));
        instanceRepository.save(offlineInstance("inst-offline", now.minus(1, ChronoUnit.HOURS)));

        List<Instance> offline = service.listOfflineInstances();

        assertThat(offline).extracting(Instance::instanceId).containsExactly("inst-offline");
    }

    @Test
    @DisplayName("req-23 实例详情：按 id 返回实例")
    void shouldGetInstanceById() {
        Instance saved = instanceRepository.save(onlineInstance("inst-detail", Instant.now()));

        Instance found = service.getInstance(saved.id());

        assertThat(found.instanceId()).isEqualTo("inst-detail");
    }

    @Test
    @DisplayName("req-23 实例详情：不存在时抛 INSTANCE_002")
    void shouldThrowWhenInstanceNotFound() {
        assertThatThrownBy(() -> service.getInstance(999L))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTANCE_002);
    }

    @Test
    @DisplayName("历史实例查询：返回全部历史实例")
    void shouldListHistoryInstances() {
        Instant now = Instant.now();
        historyInstanceRepository.save(HistoryInstance.from(
                offlineInstance("hist-1", now.minus(2, ChronoUnit.DAYS)), now.minus(2, ChronoUnit.DAYS)));
        historyInstanceRepository.save(HistoryInstance.from(
                offlineInstance("hist-2", now.minus(1, ChronoUnit.DAYS)), now.minus(1, ChronoUnit.DAYS)));

        List<HistoryInstance> history = service.listHistoryInstances();

        assertThat(history).hasSize(2);
    }
}