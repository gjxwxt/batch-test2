package com.example.app.integration;

import com.example.app.mapper.LicenseMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多节点一致性（wp-7 / req-28）Testcontainers 双进程并发回归测试。
 *
 * <p>使用真实 PostgreSQL 12+ 容器，通过 MyBatis {@link LicenseMapper} 的原子 UPDATE
 * （{@code tryAcquireInstanceQuotaAtomic}）验证双进程并发注册不超卖：单条
 * {@code UPDATE license SET used_instances = used_instances + 1 ... WHERE used_instances < max_instances * ?}
 * 在 PostgreSQL 中原子执行，配额校验与计数递增在同一事务内完成。</p>
 *
 * <p>本测试直接驱动生产 {@link LicenseMapper}（与 {@code LicenseRepository.tryAcquireInstanceQuota}
 * 在 Spring 上下文中的实现一致），而非独立 SQL 片段。依赖 Docker；当 Docker 不可用时由
 * {@code @Testcontainers(disabledWithoutDocker = true)} 自动跳过，不影响本地无 Docker 环境的构建。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class QuotaConcurrencyIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:12-alpine")
            .withDatabaseName("ias_auth")
            .withUsername("test")
            .withPassword("test");

    private static SqlSessionFactory sqlSessionFactory;
    private static String jdbcUrl;
    private static String jdbcUser;
    private static String jdbcPassword;

    @BeforeAll
    static void setUpDatabase() throws SQLException {
        jdbcUrl = POSTGRES.getJdbcUrl();
        jdbcUser = POSTGRES.getUsername();
        jdbcPassword = POSTGRES.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
             Statement stmt = conn.createStatement()) {
            // 精简 license 表结构（仅保留配额相关列），与 V1__init_schema.sql 语义一致
            stmt.execute("""
                    CREATE TABLE license (
                        id                  BIGSERIAL PRIMARY KEY,
                        serial              VARCHAR(128) NOT NULL,
                        max_instances       INTEGER      NOT NULL DEFAULT 0,
                        used_instances      INTEGER      NOT NULL DEFAULT 0,
                        remaining_instances INTEGER      NOT NULL DEFAULT 0,
                        update_time         TIMESTAMPTZ  NOT NULL DEFAULT now()
                    )
                    """);
            stmt.execute("CREATE UNIQUE INDEX uk_license_serial ON license (serial)");
            // 基础配额 10，弹性倍数 2 => 上限 20
            stmt.execute("""
                    INSERT INTO license (serial, max_instances, used_instances, remaining_instances)
                    VALUES ('serial-it-001', 10, 0, 10)
                    """);
        }

        // 用 MyBatis 将生产 LicenseMapper 绑定到 Testcontainers PostgreSQL
        DataSource dataSource = new org.apache.ibatis.datasource.unpooled.UnpooledDataSource(
                "org.postgresql.Driver", jdbcUrl, jdbcUser, jdbcPassword);
        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(LicenseMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @AfterAll
    static void tearDownDatabase() {
        // 容器由 Testcontainers 自动回收
    }

    @Test
    @DisplayName("req-28 双进程并发注册：生产 LicenseMapper 原子 SQL 配额不超卖（上限 20，并发 40 仅 20 成功）")
    void shouldNotOversellUnderDualProcessConcurrency() throws Exception {
        int elasticMultiplier = 2;
        int elasticLimit = 20; // 10 * 2
        int totalRequests = 40;

        // 模拟两个独立进程（节点）：各自持有独立 SqlSession（独立 JDBC 连接）
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch ready = new CountDownLatch(totalRequests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return acquireQuota("serial-it-001", elasticMultiplier);
            }));
        }

        ready.await();
        start.countDown();

        int success = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(60, TimeUnit.SECONDS)) {
                success++;
            }
        }
        executor.shutdownNow();

        assertThat(success).isEqualTo(elasticLimit);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT used_instances FROM license WHERE serial = ?")) {
            ps.setString(1, "serial-it-001");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt("used_instances")).isEqualTo(elasticLimit);
            }
        }
    }

    /**
     * 通过生产 {@link LicenseMapper#tryAcquireInstanceQuotaAtomic} 原子占用配额。
     * 每个调用持有独立 SqlSession（独立 JDBC 连接），模拟多节点并发。
     */
    private boolean acquireQuota(String serial, int elasticMultiplier) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            LicenseMapper mapper = session.getMapper(LicenseMapper.class);
            Long licenseId = session.selectOne(
                    "com.example.app.mapper.LicenseMapper.selectIdBySerial", serial);
            if (licenseId == null) {
                return false;
            }
            return mapper.tryAcquireInstanceQuotaAtomic(licenseId, elasticMultiplier) == 1;
        } catch (Exception e) {
            return false;
        }
    }
}