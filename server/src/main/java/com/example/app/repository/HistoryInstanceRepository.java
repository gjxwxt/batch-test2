package com.example.app.repository;

import com.example.app.model.HistoryInstance;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 历史实例仓储（内存实现）。
 *
 * <p>对应共享契约 6 表 DDL 中的 {@code history_instance} 表（infra:ddl）。
 * 由归档调度（req-19）写入、历史清理（req-20）删除、历史查询（IAS_AUTH_INST_HISTORY）读取。</p>
 */
@Repository
public class HistoryInstanceRepository {

    private final ConcurrentMap<Long, HistoryInstance> storage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> instanceIdIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public List<HistoryInstance> findAll() {
        List<HistoryInstance> instances = new ArrayList<>(storage.values());
        instances.sort(Comparator.comparing(HistoryInstance::archivedTime).reversed());
        return instances;
    }

    public Optional<HistoryInstance> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<HistoryInstance> findByInstanceId(String instanceId) {
        Long id = instanceIdIndex.get(instanceId);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    public HistoryInstance save(HistoryInstance instance) {
        HistoryInstance toStore = instance;
        if (instance.id() == null) {
            toStore = new HistoryInstance(
                    idSequence.getAndIncrement(),
                    instance.instanceId(),
                    instance.licenseId(),
                    instance.clientUuid(),
                    instance.proname(),
                    instance.productType(),
                    instance.productVersion(),
                    instance.productSpec(),
                    instance.hostname(),
                    instance.ipAddress(),
                    instance.mac(),
                    instance.machineType(),
                    instance.currentCpus(),
                    instance.currentMemory(),
                    instance.extendedAttributes(),
                    instance.status(),
                    instance.onlineTime(),
                    instance.lastHeartbeatTime(),
                    instance.offlineTime(),
                    instance.archivedTime(),
                    instance.createTime(),
                    instance.updateTime()
            );
        }
        storage.put(toStore.id(), toStore);
        instanceIdIndex.put(toStore.instanceId(), toStore.id());
        return toStore;
    }

    public boolean deleteById(Long id) {
        HistoryInstance removed = storage.remove(id);
        if (removed != null) {
            instanceIdIndex.remove(removed.instanceId());
            return true;
        }
        return false;
    }

    public boolean existsById(Long id) {
        return storage.containsKey(id);
    }

    public void clear() {
        storage.clear();
        instanceIdIndex.clear();
    }
}