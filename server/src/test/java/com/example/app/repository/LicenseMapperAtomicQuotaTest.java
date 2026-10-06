package com.example.app.repository;

import com.example.app.mapper.LicenseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * req-28 多节点配额强一致（原子 SQL/CAS）回归测试。
 *
 * <p>在 H2（PostgreSQL 兼容模式，Flyway 已建 license 表）上验证 {@link LicenseMapper}
 * 的原子 UPDATE 配额占用：单条 {@code UPDATE ... WHERE id=? AND (max_instances<=0 OR
 * used_instances < max_instances*?)} 在数据库中原子执行，多线程并发注册不超卖。
 * 该 SQL 与生产 PostgreSQL（application-prod.yml）完全一致，证明 req-28 原子 SQL/CAS
 * 行为在无 Docker 环境下亦可验证。</p>
 */
@SpringBootTest
class LicenseMapperAtomicQuotaTest {

    @Autowired
    private LicenseMapper licenseMapper;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("req-28 原子 SQL 配额：并发 40 次仅弹性上限 20 成功，不超卖")
    void shouldNotOversellWithAtomicSql() throws Exception {
        // 基础配额 10，弹性倍数 2 => 上限 20
        jdbcTemplate.update("""
                INSERT INTO license (serial, max_instances, used_instances, remaining_instances, status)
                VALUES (?, ?, 0, 10, 1)
                """, "serial-atomic-001", 10);
        Long licenseId = jdbcTemplate.queryForObject(
                "SELECT id FROM license WHERE serial = ?", Long.class, "serial-atomic-001");

        int elasticMultiplier = 2;
        int elasticLimit = 20;
        int totalRequests = 40;

        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch ready = new CountDownLatch(totalRequests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return licenseRepository.tryAcquireInstanceQuota(licenseId, elasticMultiplier);
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
        Integer used = jdbcTemplate.queryForObject(
                "SELECT used_instances FROM license WHERE id = ?", Integer.class, licenseId);
        assertThat(used).isEqualTo(elasticLimit);
    }

    @Test
    @DisplayName("req-28 原子 SQL 配额：max_instances<=0 表示不限制，恒可占用")
    void shouldAlwaysAcquireWhenUnlimited() {
        jdbcTemplate.update("""
                INSERT INTO license (serial, max_instances, used_instances, remaining_instances, status)
                VALUES (?, 0, 0, 0, 1)
                """, "serial-atomic-unlimited");
        Long licenseId = jdbcTemplate.queryForObject(
                "SELECT id FROM license WHERE serial = ?", Long.class, "serial-atomic-unlimited");

        boolean acquired = licenseRepository.tryAcquireInstanceQuota(licenseId, 2);

        assertThat(acquired).isTrue();
        Integer used = jdbcTemplate.queryForObject(
                "SELECT used_instances FROM license WHERE id = ?", Integer.class, licenseId);
        assertThat(used).isEqualTo(1);
    }
}