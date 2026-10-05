package com.example.app.service;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.license.CommunicationKeyProvider;
import com.example.app.license.CommunicationSignature;
import com.example.app.model.Instance;
import com.example.app.model.License;
import com.example.app.model.RegisterRequest;
import com.example.app.model.RegisterResponse;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.LicenseRepository;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.impl.LicenseClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 弹性配额与多节点一致性（wp-7）单元测试（TDD）。
 *
 * <p>覆盖 req-27（弹性配额倍数可配置）与 req-28（原子 CAS 配额，多线程并发不超卖）。</p>
 */
class ElasticQuotaServiceTest {

    private LicenseRepository licenseRepository;
    private InstanceRepository instanceRepository;
    private SystemConfigRepository systemConfigRepository;
    private CommunicationKeyProvider keyProvider;
    private LicenseClientService service;
    private PrivateKey privateKey;

    @BeforeEach
    void setUp() {
        licenseRepository = new LicenseRepository();
        instanceRepository = new InstanceRepository();
        systemConfigRepository = new SystemConfigRepository();
        keyProvider = new CommunicationKeyProvider(null);
        privateKey = keyProvider.getPrivateKey();
        service = new LicenseClientServiceImpl(keyProvider, licenseRepository, instanceRepository, systemConfigRepository);
    }

    @Test
    @DisplayName("req-27 默认弹性倍数 2：基础配额 5 可注册至 10 个实例")
    void shouldAllowUpToDefaultElasticLimit() {
        seedLicense("serial-elastic-default", 5);

        for (int i = 0; i < 10; i++) {
            RegisterResponse response = service.register(signedRegister("serial-elastic-default", "inst-" + i));
            assertThat(response.status()).isEqualTo(Instance.STATUS_ONLINE);
        }

        License updated = licenseRepository.findBySerial("serial-elastic-default").orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(10);
    }

    @Test
    @DisplayName("req-27 默认弹性倍数 2：超过 2 倍上限（第 11 个）拒绝注册 INSTANCE_004")
    void shouldRejectBeyondDefaultElasticLimit() {
        seedLicense("serial-elastic-over", 5);

        for (int i = 0; i < 10; i++) {
            service.register(signedRegister("serial-elastic-over", "inst-" + i));
        }

        assertThatThrownBy(() -> service.register(signedRegister("serial-elastic-over", "inst-over")))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTANCE_004);
    }

    @Test
    @DisplayName("req-27 弹性倍数可配置为 3：基础配额 5 可注册至 15 个实例")
    void shouldHonorConfiguredElasticMultiplier() {
        systemConfigRepository.put(SystemConfigRepository.KEY_ELASTIC_QUOTA_MULTIPLIER, "3");
        seedLicense("serial-elastic-cfg", 5);

        for (int i = 0; i < 15; i++) {
            RegisterResponse response = service.register(signedRegister("serial-elastic-cfg", "inst-" + i));
            assertThat(response.status()).isEqualTo(Instance.STATUS_ONLINE);
        }

        License updated = licenseRepository.findBySerial("serial-elastic-cfg").orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(15);
    }

    @Test
    @DisplayName("req-27 弹性倍数配置为 3：超过 3 倍上限（第 16 个）拒绝注册 INSTANCE_004")
    void shouldRejectBeyondConfiguredElasticLimit() {
        systemConfigRepository.put(SystemConfigRepository.KEY_ELASTIC_QUOTA_MULTIPLIER, "3");
        seedLicense("serial-elastic-cfg-over", 5);

        for (int i = 0; i < 15; i++) {
            service.register(signedRegister("serial-elastic-cfg-over", "inst-" + i));
        }

        assertThatThrownBy(() -> service.register(signedRegister("serial-elastic-cfg-over", "inst-over")))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTANCE_004);
    }

    @Test
    @DisplayName("req-27 弹性倍数配置非法时回退默认 2")
    void shouldFallbackToDefaultWhenMultiplierInvalid() {
        systemConfigRepository.put(SystemConfigRepository.KEY_ELASTIC_QUOTA_MULTIPLIER, "not-a-number");
        seedLicense("serial-elastic-invalid", 5);

        for (int i = 0; i < 10; i++) {
            service.register(signedRegister("serial-elastic-invalid", "inst-" + i));
        }

        assertThatThrownBy(() -> service.register(signedRegister("serial-elastic-invalid", "inst-over")))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTANCE_004);
    }

    @Test
    @DisplayName("req-27 max_instances<=0 表示不限制实例数，可无限注册")
    void shouldNotLimitWhenMaxInstancesZero() {
        seedLicense("serial-elastic-unlimited", 0);

        for (int i = 0; i < 50; i++) {
            RegisterResponse response = service.register(signedRegister("serial-elastic-unlimited", "inst-" + i));
            assertThat(response.status()).isEqualTo(Instance.STATUS_ONLINE);
        }

        License updated = licenseRepository.findBySerial("serial-elastic-unlimited").orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(50);
    }

    @Test
    @DisplayName("req-28 多线程并发注册不超卖：基础配额 10 弹性 2 倍，并发 30 请求仅 20 成功")
    void shouldNotOversellUnderConcurrency() throws Exception {
        seedLicense("serial-cas-concurrent", 10);
        int elasticLimit = 20; // 10 * 2
        int totalRequests = 30;

        // 线程池大小 = 请求数，避免任务阻塞在 start 闩上时池内线程耗尽导致死锁
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch ready = new CountDownLatch(totalRequests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    service.register(signedRegister("serial-cas-concurrent", "inst-c-" + idx));
                    return true;
                } catch (LicenseException e) {
                    return false;
                }
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

        License updated = licenseRepository.findBySerial("serial-cas-concurrent").orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(elasticLimit);
        assertThat(instanceRepository.findAll().size()).isEqualTo(elasticLimit);
    }

    // ---- helpers ----

    private License seedLicense(String serial, int maxInstances) {
        Instant now = Instant.now();
        License license = new License(
                null, serial, "Test License", "AS", "Server", "1.0", "Test Corp",
                "center", "true", "never", "test-user", maxInstances, 8, 16384,
                0, maxInstances, 0, 0, "<license><serial>" + serial + "</serial></license>",
                License.STATUS_ACTIVE, "FILE", now, now);
        return licenseRepository.save(license);
    }

    private RegisterRequest signedRegister(String serial, String clientUuid) {
        // canonical 与 LicenseClientServiceImpl.buildRegisterCanonical 保持一致：
        // serial + clientUuid + proname + productType + productVersion + productSpec
        //   + hostname + ipAddress + mac + machineType
        String canonical = serial + clientUuid + "AS" + "" + "" + ""
                + "host-1" + "192.168.1.10" + "AA:BB:CC" + "VM";
        String signature = CommunicationSignature.sign(canonical, privateKey);
        return new RegisterRequest(
                serial, clientUuid, "AS", null, null, null,
                "host-1", "192.168.1.10", "AA:BB:CC", "VM", 4, 8192, null, signature);
    }
}