package com.example.app.model;

import java.time.Instant;

/**
 * 审计日志记录。
 * 对应 schema 中 audit_log 表。
 */
public record AuditLog(
        String logId,
        AuditOperationType operationType,
        String operationDesc,
        String operator,
        String operatorIp,
        String targetId,
        String result,
        String detail,
        Instant operateTime
) {}