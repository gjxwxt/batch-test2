package com.example.app.service.impl;

import com.example.app.model.AuditLog;
import com.example.app.model.AuditLogQuery;
import com.example.app.model.AuditOperationType;
import com.example.app.repository.AuditLogRepository;
import com.example.app.service.AuditService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog record(AuditOperationType operationType, String operator, String operatorIp,
                           String targetId, String result, String detail) {
        String logId = "log-" + UUID.randomUUID().toString().substring(0, 8);
        AuditLog log = new AuditLog(
                logId,
                operationType,
                operationType.name(),
                operator,
                operatorIp,
                targetId,
                result,
                detail,
                Instant.now()
        );
        return auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> query(AuditLogQuery query) {
        return auditLogRepository.findByQuery(query);
    }
}