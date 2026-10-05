package com.example.app.repository;

import com.example.app.model.License;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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