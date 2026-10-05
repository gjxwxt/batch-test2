package com.example.app.repository;

import com.example.app.model.Instance;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 在线实例仓储（内存实现）。
 *
 * <p>后续可替换为 MyBatis-Plus + PostgreSQL 实现（infra:scaffold）。
 * 心跳时间维护在内存并批量落库（需求约束）。</p>
 */
@Repository
public class InstanceRepository {

    private final ConcurrentMap<Long, Instance> storage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> instanceIdIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public List<Instance> findAll() {
        List<Instance> instances = new ArrayList<>(storage.values());
        instances.sort(Comparator.comparing(Instance::createTime).reversed());
        return instances;
    }

    public Optional<Instance> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<Instance> findByInstanceId(String instanceId) {
        Long id = instanceIdIndex.get(instanceId);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    public boolean existsByInstanceId(String instanceId) {
        return instanceIdIndex.containsKey(instanceId);
    }

    public Instance save(Instance instance) {
        Instance toStore = instance;
        if (instance.id() == null) {
            toStore = new Instance(
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
                    instance.createTime(),
                    instance.updateTime()
            );
        }
        storage.put(toStore.id(), toStore);
        instanceIdIndex.put(toStore.instanceId(), toStore.id());
        return toStore;
    }

    public boolean deleteById(Long id) {
        Instance removed = storage.remove(id);
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