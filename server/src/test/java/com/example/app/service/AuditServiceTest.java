package com.example.app.service;

import com.example.app.model.AuditLog;
import com.example.app.model.AuditLogQuery;
import com.example.app.model.AuditOperationType;
import com.example.app.repository.AuditLogRepository;
import com.example.app.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl(auditLogRepository);
    }

    @Test
    @DisplayName("record generates log id, captures operator/ip and persists audit log")
    void shouldRecordAuditLogWithGeneratedId() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog recorded = auditService.record(
                AuditOperationType.LOGIN,
                "admin",
                "192.168.1.10",
                "admin-1",
                "SUCCESS",
                "管理员登录成功"
        );

        assertThat(recorded.logId()).isNotBlank();
        assertThat(recorded.operationType()).isEqualTo(AuditOperationType.LOGIN);
        assertThat(recorded.operator()).isEqualTo("admin");
        assertThat(recorded.operatorIp()).isEqualTo("192.168.1.10");
        assertThat(recorded.targetId()).isEqualTo("admin-1");
        assertThat(recorded.result()).isEqualTo("SUCCESS");
        assertThat(recorded.detail()).isEqualTo("管理员登录成功");
        assertThat(recorded.operateTime()).isNotNull();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().operationType()).isEqualTo(AuditOperationType.LOGIN);
    }

    @Test
    @DisplayName("query returns audit logs filtered by operation type")
    void shouldQueryAuditLogsByOperationType() {
        AuditLog loginLog = new AuditLog("log-1", AuditOperationType.LOGIN, "管理员登录",
                "admin", "192.168.1.10", "admin-1", "SUCCESS", "登录成功", Instant.now());
        AuditLog deleteLog = new AuditLog("log-2", AuditOperationType.LICENSE_DELETE, "删除授权",
                "admin", "192.168.1.10", "lic-1", "SUCCESS", "删除授权", Instant.now());
        when(auditLogRepository.findByQuery(any(AuditLogQuery.class)))
                .thenReturn(List.of(loginLog));

        List<AuditLog> result = auditService.query(new AuditLogQuery(AuditOperationType.LOGIN, null, null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).operationType()).isEqualTo(AuditOperationType.LOGIN);
        verify(auditLogRepository, times(1)).findByQuery(any(AuditLogQuery.class));
    }

    @Test
    @DisplayName("query without filters returns all audit logs")
    void shouldQueryAllAuditLogsWhenNoFilter() {
        AuditLog loginLog = new AuditLog("log-1", AuditOperationType.LOGIN, "管理员登录",
                "admin", "192.168.1.10", "admin-1", "SUCCESS", "登录成功", Instant.now());
        when(auditLogRepository.findByQuery(any(AuditLogQuery.class)))
                .thenReturn(List.of(loginLog));

        List<AuditLog> result = auditService.query(new AuditLogQuery(null, null, null, null));

        assertThat(result).hasSize(1);
        verify(auditLogRepository, times(1)).findByQuery(any(AuditLogQuery.class));
    }

    @Test
    @DisplayName("AuditOperationType exposes all audit event types (13 core + extended)")
    void shouldExposeAllAuditEventTypes() {
        assertThat(AuditOperationType.values())
                .containsExactlyInAnyOrder(
                        AuditOperationType.LOGIN,
                        AuditOperationType.CHANGE_PASSWORD,
                        AuditOperationType.LICENSE_IMPORT,
                        AuditOperationType.LICENSE_DELETE,
                        AuditOperationType.LICENSE_DISABLE,
                        AuditOperationType.LICENSE_VERIFY,
                        AuditOperationType.LICENSE_QUERY,
                        AuditOperationType.INSTANCE_OFFLINE,
                        AuditOperationType.INSTANCE_QUERY,
                        AuditOperationType.CONFIG_UPDATE,
                        AuditOperationType.CONFIG_RELOAD,
                        AuditOperationType.AUDIT_QUERY,
                        AuditOperationType.STATISTICS_QUERY,
                        AuditOperationType.REGISTER,
                        AuditOperationType.HEARTBEAT,
                        AuditOperationType.FILE_APPLY,
                        AuditOperationType.TIMEOUT,
                        AuditOperationType.ARCHIVE,
                        AuditOperationType.INSTANCE_DELETE,
                        AuditOperationType.INSTANCE_RECOVER
                );
    }
}