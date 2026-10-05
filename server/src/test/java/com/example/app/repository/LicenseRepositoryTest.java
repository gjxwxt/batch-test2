package com.example.app.repository;

import com.example.app.model.License;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 授权仓储原子配额（wp-7 / req-28）单元测试。
 *
 * <p>直接验证 {@link LicenseRepository#tryAcquireInstanceQuota} 的 CAS 语义：
 * 单次原子操作内完成配额校验与计数更新，多线程并发不超卖。</p>
 */
class LicenseRepositoryTest {

    private LicenseRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LicenseRepository();
    }

    @Test
    @DisplayName("配额未满时原子占用成功并递增 used_instances")
    void shouldAcquireWhenQuotaAvailable() {
        License license = seedLicense("serial-repo-1", 5);

        boolean acquired = repository.tryAcquireInstanceQuota(license.id(), 2);

        assertThat(acquired).isTrue();
        License updated = repository.findById(license.id()).orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(1);
        assertThat(updated.remainingInstances()).isEqualTo(4);
    }

    @Test
    @DisplayName("配额达到弹性上限时占用失败且计数不变")
    void shouldRejectWhenQuotaExhausted() {
        License license = seedLicense("serial-repo-2", 5);
        // 预置 used=10（= 5*2 弹性上限）
        repository.save(withUsed(license, 10, 0));

        boolean acquired = repository.tryAcquireInstanceQuota(license.id(), 2);

        assertThat(acquired).isFalse();
        License updated = repository.findById(license.id()).orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(10);
    }

    @Test
    @DisplayName("max_instances<=0 表示不限制，恒可占用")
    void shouldAlwaysAcquireWhenUnlimited() {
        License license = seedLicense("serial-repo-3", 0);

        boolean acquired = repository.tryAcquireInstanceQuota(license.id(), 2);

        assertThat(acquired).isTrue();
        License updated = repository.findById(license.id()).orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(1);
    }

    @Test
    @DisplayName("多线程并发占用不超卖：上限 20，并发 50 次仅 20 成功")
    void shouldNotOversellUnderConcurrentAcquire() throws Exception {
        License license = seedLicense("serial-repo-concurrent", 10);
        int elasticLimit = 20;
        int total = 50;

        // 线程池大小 = 请求数，避免任务阻塞在 start 闩上时池内线程耗尽导致死锁
        ExecutorService executor = Executors.newFixedThreadPool(total);
        CountDownLatch ready = new CountDownLatch(total);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return repository.tryAcquireInstanceQuota(license.id(), elasticLimit / 10);
            }));
        }

        ready.await();
        start.countDown();

        int success = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(30, TimeUnit.SECONDS)) {
                success++;
            }
        }
        executor.shutdownNow();

        assertThat(success).isEqualTo(elasticLimit);
        License updated = repository.findById(license.id()).orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(elasticLimit);
    }

    // ---- helpers ----

    private License seedLicense(String serial, int maxInstances) {
        Instant now = Instant.now();
        License license = new License(
                null, serial, "Test License", "AS", "Server", "1.0", "Test Corp",
                "center", "true", "never", "test-user", maxInstances, 8, 16384,
                0, maxInstances, 0, 0, "<license><serial>" + serial + "</serial></license>",
                License.STATUS_ACTIVE, "FILE", now, now);
        return repository.save(license);
    }

    private License withUsed(License license, int used, int remaining) {
        return new License(
                license.id(), license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(), license.licenseMode(),
                license.formal(), license.expiration(), license.userinfor(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), used, remaining,
                license.usedCpus(), license.usedMemory(), license.bxbFile(), license.status(),
                license.source(), license.createTime(), license.updateTime());
    }
}