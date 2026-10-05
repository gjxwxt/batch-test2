package com.example.app.repository;

import com.example.app.model.License;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 授权仓储（内存实现）。
 *
 * <p>后续可替换为 MyBatis-Plus + PostgreSQL 实现（infra:scaffold）。
 * 配额计数以数据库为准（req-28 强一致）。</p>
 */
@Repository
public class LicenseRepository {

    private final ConcurrentMap<Long, License> storage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> serialIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public List<License> findAll() {
        List<License> licenses = new ArrayList<>(storage.values());
        licenses.sort(Comparator.comparing(License::createTime).reversed());
        return licenses;
    }

    public Optional<License> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<License> findBySerial(String serial) {
        Long id = serialIndex.get(serial);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    public boolean existsBySerial(String serial) {
        return serialIndex.containsKey(serial);
    }

    public License save(License license) {
        License toStore = license;
        if (license.id() == null) {
            toStore = new License(
                    idSequence.getAndIncrement(),
                    license.serial(),
                    license.licenseName(),
                    license.proname(),
                    license.component(),
                    license.version(),
                    license.licensee(),
                    license.licenseMode(),
                    license.formal(),
                    license.expiration(),
                    license.userinfor(),
                    license.maxInstances(),
                    license.maxCpus(),
                    license.maxMemory(),
                    license.usedInstances(),
                    license.remainingInstances(),
                    license.usedCpus(),
                    license.usedMemory(),
                    license.bxbFile(),
                    license.status(),
                    license.source(),
                    license.createTime(),
                    license.updateTime()
            );
        }
        storage.put(toStore.id(), toStore);
        serialIndex.put(toStore.serial(), toStore.id());
        return toStore;
    }

    /**
     * 原子占用一个实例配额（req-28 多节点一致性）。
     *
     * <p>基于 {@link ConcurrentHashMap#computeIfPresent} 的 per-key 原子性实现 CAS 语义：
     * 在单次原子操作内完成「配额上限校验 + used_instances 递增 + remaining_instances 递减」，
     * 避免多节点并发注册时读改写（read-modify-write）竞态导致配额超卖。</p>
     *
     * <p>弹性配额（req-27）：允许实例数超过基础配额，上限为 {@code max_instances * elasticMultiplier}。
     * 当 {@code max_instances <= 0} 表示不限制实例数，恒可占用。</p>
     *
     * @param licenseId         授权主键
     * @param elasticMultiplier 弹性配额倍数（来自 system_config.elastic.quota.multiplier）
     * @return 占用成功返回 {@code true}；配额已满返回 {@code false}
     */
    public boolean tryAcquireInstanceQuota(Long licenseId, int elasticMultiplier) {
        AtomicBoolean acquired = new AtomicBoolean(false);
        storage.computeIfPresent(licenseId, (id, license) -> {
            int maxInstances = license.maxInstances() != null ? license.maxInstances() : 0;
            int used = license.usedInstances() != null ? license.usedInstances() : 0;
            int remaining = license.remainingInstances() != null ? license.remainingInstances() : 0;

            // 弹性配额上限：max_instances * multiplier；max_instances <= 0 表示不限制
            long elasticLimit = (long) maxInstances * elasticMultiplier;
            if (maxInstances > 0 && used >= elasticLimit) {
                acquired.set(false);
                return license; // 配额已满，不修改
            }

            acquired.set(true);
            return new License(
                    license.id(),
                    license.serial(),
                    license.licenseName(),
                    license.proname(),
                    license.component(),
                    license.version(),
                    license.licensee(),
                    license.licenseMode(),
                    license.formal(),
                    license.expiration(),
                    license.userinfor(),
                    license.maxInstances(),
                    license.maxCpus(),
                    license.maxMemory(),
                    used + 1,
                    Math.max(0, remaining - 1),
                    license.usedCpus(),
                    license.usedMemory(),
                    license.bxbFile(),
                    license.status(),
                    license.source(),
                    license.createTime(),
                    Instant.now()
            );
        });
        return acquired.get();
    }

    public boolean deleteById(Long id) {
        License removed = storage.remove(id);
        if (removed != null) {
            serialIndex.remove(removed.serial());
            return true;
        }
        return false;
    }

    public boolean existsById(Long id) {
        return storage.containsKey(id);
    }

    public void clear() {
        storage.clear();
        serialIndex.clear();
    }
}