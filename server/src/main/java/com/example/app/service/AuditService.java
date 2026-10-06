package com.example.app.service;

import com.example.app.model.AuditLog;
import com.example.app.model.AuditLogQuery;
import com.example.app.model.AuditOperationType;

import java.util.List;

public interface AuditService {

    AuditLog record(AuditOperationType operationType, String operator, String operatorIp,
                    String targetId, String result, String detail);

    List<AuditLog> query(AuditLogQuery query);
}