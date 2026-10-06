package com.example.app.repository;

import com.example.app.model.AuditLog;
import com.example.app.model.AuditLogQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 审计日志仓储（内存实现）。
 */
@Repository
public class AuditLogRepository {

    private final ConcurrentMap<String, AuditLog> storage = new ConcurrentHashMap<>();

    public AuditLog save(AuditLog log) {
        storage.put(log.logId(), log);
        return log;
    }

    public List<AuditLog> findByQuery(AuditLogQuery query) {
        List<AuditLog> result = new ArrayList<>();
        for (AuditLog log : storage.values()) {
            if (matches(log, query)) {
                result.add(log);
            }
        }
        result.sort(Comparator.comparing(AuditLog::operateTime).reversed());
        return result;
    }

public List<AuditLog> findAll() {
        List<AuditLog> result = new ArrayList<>(storage.values());
        result.sort(Comparator.comparing(AuditLog::operateTime).reversed());
        return result;
    }

    public void clear() {
        storage.clear();
    }

    private boolean matches(AuditLog log, AuditLogQuery query) {
        if (query == null) {
            return true;
        }
        if (query.operationType() != null && query.operationType() != log.operationType()) {
            return false;
        }
        if (query.operator() != null && !query.operator().equals(log.operator())) {
            return false;
        }
        if (query.result() != null && !query.result().equals(log.result())) {
            return false;
        }
        if (query.targetId() != null && !query.targetId().equals(log.targetId())) {
            return false;
        }
        return true;
    }
}