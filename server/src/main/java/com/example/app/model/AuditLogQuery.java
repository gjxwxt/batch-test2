package com.example.app.model;

/**
 * 审计日志查询条件。
 */
public record AuditLogQuery(
        AuditOperationType operationType,
        String operator,
        String result,
        String targetId
) {}