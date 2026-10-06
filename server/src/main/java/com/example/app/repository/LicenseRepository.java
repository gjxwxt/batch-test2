package com.example.app.repository;

import com.example.app.mapper.LicenseMapper;
import com.example.app.model.License;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 授权仓储。
 *
 * <p>配额计数（used_instances / remaining_instances / used_cpus / used_memory）以数据库为准
 * （req-28 强一致）。当运行于 Spring 上下文（存在 {@link LicenseMapper}，即已配置数据源）时，
 * 配额占用走 MyBatis-Plus 原子 SQL/CAS（单条 UPDATE 在 PostgreSQL/H2-PG 中原子执行，多节点
 * 共享库强一致）；否则（纯单元测试直接 new 实例）回退到内存 {@link ConcurrentHashMap} CAS 语义。</p>
 */
@Repository
public class LicenseRepository {

    private final ConcurrentMap<Long, License> storage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> serialIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    /** 可选的 MyBatis-Plus Mapper；Spring 上下文注入后配额占用走原子 SQL（req-28）。 */
    private LicenseMapper licenseMapper;

    @Autowired(required = false)
    public void setLicenseMapper(LicenseMapper licenseMapper) {
        this.licenseMapper = licenseMapper;
    }

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
     * <p>Spring 上下文（存在 {@link LicenseMapper}）时走 MyBatis-Plus 原子 SQL/CAS：
     * 单条 {@code UPDATE ... WHERE id=? AND (max_instances<=0 OR used_instances < max_instances*?)}
     * 在数据库（PostgreSQL 12+ / H2-PG）中原子执行，配额校验与计数递增不拆分，多节点共享库强一致。</p>
     *
     * <p>纯单元测试（无 Mapper）时回退到 {@link ConcurrentHashMap#computeIfPresent} 的 per-key
     * 原子性实现 CAS 语义，保证单 JVM 内并发不超卖。</p>
     *
     * <p>弹性配额（req-27）：允许实例数超过基础配额，上限为 {@code max_instances * elasticMultiplier}。
     * 当 {@code max_instances <= 0} 表示不限制实例数，恒可占用。</p>
     *
     * @param licenseId         授权主键
     * @param elasticMultiplier 弹性配额倍数（来自 system_config.elastic.quota.multiplier）
     * @return 占用成功返回 {@code true}；配额已满返回 {@code false}
     */
    public boolean tryAcquireInstanceQuota(Long licenseId, int elasticMultiplier) {
        if (licenseMapper != null) {
            return licenseMapper.tryAcquireInstanceQuotaAtomic(licenseId, elasticMultiplier) == 1;
        }
        AtomicBoolean acquired = new AtomicBoolean(false);
        storage.computeIfPresent(licenseId, (id, license) -> {
            int maxInstances = license.maxInstances() != null ? license.maxInstances() : 0;
            int used = license.usedInstances() != null ? license.usedInstances() : 0;
            int remaining = license.remainingInstances() != null ? license.remainingInstances() : 0;

            // 弹性配额上限：max_instances * multiplier；max_instances <= 0 表示不限制。
            // 判定基于占用前 used：used >= max*2 拒绝（AUTH-056），used < max*2 允许（AUTH-054/055）。
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

    /**
     * 原子占用 CPU/内存配额（AUTH-030/031）。
     *
     * <p>Spring 上下文（存在 {@link LicenseMapper}）时走 MyBatis-Plus 原子 SQL UPDATE；
     * 否则回退到内存 {@link ConcurrentHashMap#computeIfPresent} CAS 语义。</p>
     *
     * @param licenseId 授权主键
     * @param cpus      本次占用的 CPU 数（可空）
     * @param memory    本次占用的内存 MB（可空）
     */
    public void acquireCpuMemoryQuota(Long licenseId, Integer cpus, Integer memory) {
        if (licenseMapper != null) {
            licenseMapper.acquireCpuMemoryQuotaAtomic(licenseId, cpus != null ? cpus : 0, memory != null ? memory : 0);
            return;
        }
        storage.computeIfPresent(licenseId, (id, license) -> {
            int usedCpus = license.usedCpus() != null ? license.usedCpus() : 0;
            int usedMemory = license.usedMemory() != null ? license.usedMemory() : 0;
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
                    license.usedInstances(),
                    license.remainingInstances(),
                    usedCpus + (cpus != null ? cpus : 0),
                    usedMemory + (memory != null ? memory : 0),
                    license.bxbFile(),
                    license.status(),
                    license.source(),
                    license.createTime(),
                    Instant.now()
            );
        });
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