package com.example.app.service;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.license.CommunicationKeyProvider;
import com.example.app.license.CommunicationSignature;
import com.example.app.license.LicenseFileGenerator;
import com.example.app.license.LicenseSigningKeyProvider;
import com.example.app.model.FileApplyRequest;
import com.example.app.model.FileApplyResponse;
import com.example.app.model.HeartbeatConfigResponse;
import com.example.app.model.HeartbeatRequest;
import com.example.app.model.HeartbeatResponse;
import com.example.app.model.Instance;
import com.example.app.model.License;
import com.example.app.model.PublicKeyResponse;
import com.example.app.model.RegisterRequest;
import com.example.app.model.RegisterResponse;
import com.example.app.repository.AuditLogRepository;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.LicenseRepository;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.impl.AuditServiceImpl;
import com.example.app.service.impl.LicenseClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 客户端交互服务（wp-4）单元测试（TDD）。
 *
 * <p>覆盖公钥分发 / 心跳参数 / 注册 / 重注册 / 心跳 / 文件申请，以及 LICENSE_001/002/004、INSTANCE_002/004 错误码。</p>
 */
class LicenseClientServiceTest {

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
        AuditServiceImpl auditService = new AuditServiceImpl(new AuditLogRepository());
        LicenseFileGenerator licenseFileGenerator = new LicenseFileGenerator(new LicenseSigningKeyProvider(""));
        service = new LicenseClientServiceImpl(keyProvider, licenseRepository, instanceRepository,
                systemConfigRepository, auditService, licenseFileGenerator);
    }

    @Test
    @DisplayName("公钥分发返回 RSA-2048 公钥")
    void shouldReturnPublicKey() {
        PublicKeyResponse response = service.getPublicKey();

        assertThat(response.algorithm()).isEqualTo("RSA");
        assertThat(response.keySize()).isEqualTo(2048);
        assertThat(response.publicKey()).isNotBlank();
    }

    @Test
    @DisplayName("心跳参数返回默认间隔 30s 与超时 90s")
    void shouldReturnHeartbeatConfig() {
        HeartbeatConfigResponse response = service.getHeartbeatConfig();

        assertThat(response.heartbeatInterval()).isEqualTo(30);
        assertThat(response.timeoutCount()).isEqualTo(3);
        assertThat(response.timeoutSeconds()).isEqualTo(90);
    }

    @Test
    @DisplayName("注册有效授权成功，创建 ONLINE 实例并占用配额")
    void shouldRegisterSuccessfully() {
        License license = seedLicense("serial-reg-001", 10);
        RegisterRequest request = signedRegister("serial-reg-001", "client-uuid-1");

        RegisterResponse response = service.register(request);

        assertThat(response.instanceId()).isEqualTo("client-uuid-1");
        assertThat(response.status()).isEqualTo(Instance.STATUS_ONLINE);
        assertThat(response.heartbeatInterval()).isEqualTo(30);

        Instance instance = instanceRepository.findByInstanceId("client-uuid-1").orElseThrow();
        assertThat(instance.isOnline()).isTrue();
        assertThat(instance.licenseId()).isEqualTo(license.id());

        License updated = licenseRepository.findBySerial("serial-reg-001").orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(1);
        assertThat(updated.remainingInstances()).isEqualTo(9);
    }

    @Test
    @DisplayName("注册不存在的授权抛出 LICENSE_001")
    void shouldRejectRegisterWhenLicenseNotFound() {
        RegisterRequest request = signedRegister("serial-missing", "client-uuid-x");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_001);
    }

    @Test
    @DisplayName("注册签名被篡改抛出 LICENSE_002")
    void shouldRejectRegisterWhenSignatureInvalid() {
        seedLicense("serial-reg-sig", 10);
        RegisterRequest request = new RegisterRequest(
                "serial-reg-sig", "client-uuid-sig", "AS", null, null, null,
                null, null, null, null, 4, 8192, null, "invalid-signature");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_002);
    }

    @Test
    @DisplayName("注册已禁用授权抛出 LICENSE_004")
    void shouldRejectRegisterWhenLicenseDisabled() {
        License license = seedLicense("serial-reg-disabled", 10);
        License disabled = new License(
                license.id(), license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(), license.licenseMode(),
                license.formal(), license.expiration(), license.userinfor(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.usedInstances(), license.remainingInstances(),
                license.usedCpus(), license.usedMemory(), license.bxbFile(), License.STATUS_DISABLED,
                license.source(), license.createTime(), license.updateTime());
        licenseRepository.save(disabled);

        RegisterRequest request = signedRegister("serial-reg-disabled", "client-uuid-d");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_004);
    }

    @Test
    @DisplayName("配额超 200% 上限拒绝注册抛出 INSTANCE_004")
    void shouldRejectRegisterWhenQuotaExceeded() {
        License license = seedLicense("serial-reg-quota", 10);
        License overQuota = new License(
                license.id(), license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(), license.licenseMode(),
                license.formal(), license.expiration(), license.userinfor(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), 20, 0,
                license.usedCpus(), license.usedMemory(), license.bxbFile(), license.status(),
                license.source(), license.createTime(), license.updateTime());
        licenseRepository.save(overQuota);

        RegisterRequest request = signedRegister("serial-reg-quota", "client-uuid-q");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTANCE_004);
    }

    @Test
    @DisplayName("已存在实例再次注册执行重注册（req-17）")
    void shouldReRegisterExistingInstance() {
        seedLicense("serial-rereg", 10);
        service.register(signedRegister("serial-rereg", "client-uuid-r"));

        RegisterResponse response = service.register(signedRegister("serial-rereg", "client-uuid-r"));

        assertThat(response.message()).contains("重注册");
        assertThat(response.status()).isEqualTo(Instance.STATUS_ONLINE);
        // 重注册不重复占用配额
        License updated = licenseRepository.findBySerial("serial-rereg").orElseThrow();
        assertThat(updated.usedInstances()).isEqualTo(1);
    }

    @Test
    @DisplayName("心跳更新实例最近心跳时间")
    void shouldUpdateHeartbeat() {
        seedLicense("serial-hb", 10);
        service.register(signedRegister("serial-hb", "client-uuid-hb"));

        HeartbeatRequest request = signedHeartbeat("client-uuid-hb", "serial-hb");
        HeartbeatResponse response = service.heartbeat(request);

        assertThat(response.status()).isEqualTo(Instance.STATUS_ONLINE);
        assertThat(response.instanceId()).isEqualTo("client-uuid-hb");

        Instance instance = instanceRepository.findByInstanceId("client-uuid-hb").orElseThrow();
        assertThat(instance.lastHeartbeatTime()).isNotNull();
    }

    @Test
    @DisplayName("心跳不存在的实例抛出 INSTANCE_002")
    void shouldRejectHeartbeatWhenInstanceNotFound() {
        HeartbeatRequest request = signedHeartbeat("missing-instance", "serial-hb");

        assertThatThrownBy(() -> service.heartbeat(request))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTANCE_002);
    }

    @Test
    @DisplayName("AUTH-040 授权已禁用时心跳抛出 INSTANCE_003")
    void shouldRejectHeartbeatWhenLicenseDisabled() {
        License license = seedLicense("serial-hb-disabled", 10);
        service.register(signedRegister("serial-hb-disabled", "client-uuid-hb-d"));
        // 禁用授权
        License disabled = new License(
                license.id(), license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(), license.licenseMode(),
                license.formal(), license.expiration(), license.userinfor(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.usedInstances(),
                license.remainingInstances(), license.usedCpus(), license.usedMemory(),
                license.bxbFile(), License.STATUS_DISABLED, license.source(),
                license.createTime(), Instant.now());
        licenseRepository.save(disabled);

        HeartbeatRequest request = signedHeartbeat("client-uuid-hb-d", "serial-hb-disabled");

        assertThatThrownBy(() -> service.heartbeat(request))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSTANCE_003);
    }

    @Test
    @DisplayName("AUTH-038 下线实例恢复心跳时重新占用配额")
    void shouldReacquireQuotaOnOfflineRecovery() {
        seedLicense("serial-hb-recover", 10);
        service.register(signedRegister("serial-hb-recover", "client-uuid-hb-r"));
        // 模拟实例被标记 OFFLINE（超时下线）
        Instance online = instanceRepository.findByInstanceId("client-uuid-hb-r").orElseThrow();
        Instance offline = new Instance(
                online.id(), online.instanceId(), online.licenseId(), online.clientUuid(),
                online.proname(), online.productType(), online.productVersion(), online.productSpec(),
                online.hostname(), online.ipAddress(), online.mac(), online.machineType(),
                online.currentCpus(), online.currentMemory(), online.extendedAttributes(),
                Instance.STATUS_OFFLINE, online.onlineTime(), online.lastHeartbeatTime(),
                Instant.now(), online.createTime(), Instant.now());
        instanceRepository.save(offline);
        // 释放配额（模拟超时下线时配额释放）
        License before = licenseRepository.findBySerial("serial-hb-recover").orElseThrow();
        License released = new License(
                before.id(), before.serial(), before.licenseName(), before.proname(),
                before.component(), before.version(), before.licensee(), before.licenseMode(),
                before.formal(), before.expiration(), before.userinfor(), before.maxInstances(),
                before.maxCpus(), before.maxMemory(), 0, before.maxInstances(), 0, 0,
                before.bxbFile(), before.status(), before.source(), before.createTime(), Instant.now());
        licenseRepository.save(released);

        HeartbeatRequest request = signedHeartbeat("client-uuid-hb-r", "serial-hb-recover");
        HeartbeatResponse response = service.heartbeat(request);

        assertThat(response.status()).isEqualTo(Instance.STATUS_ONLINE);
        License after = licenseRepository.findBySerial("serial-hb-recover").orElseThrow();
        assertThat(after.usedInstances()).isEqualTo(1);
        Instance recovered = instanceRepository.findByInstanceId("client-uuid-hb-r").orElseThrow();
        assertThat(recovered.status()).isEqualTo(Instance.STATUS_ONLINE);
    }

    @Test
    @DisplayName("local 模式授权文件申请成功返回授权文件内容")
    void shouldApplyFileForLocalMode() {
        FileApplyRequest request = new FileApplyRequest(
                "local", "InforSuite AS", "企业版", "示例客户", 10, null, null);

        FileApplyResponse response = service.fileApply(request);

        assertThat(response.applyId()).isNotBlank();
        assertThat(response.mode()).isEqualTo("local");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.licenseFile()).isNotBlank();
        assertThat(response.licenseFile()).contains("<license>");
        assertThat(response.licenseFile()).contains("<mode>local</mode>");
        assertThat(response.licenseFile()).contains("<signature>");
    }

    @Test
    @DisplayName("center 模式授权文件申请抛出 PARAM_001")
    void shouldRejectFileApplyForCenterMode() {
        FileApplyRequest request = new FileApplyRequest(
                "center", "InforSuite AS", "企业版", "示例客户", 10, null, null);

        assertThatThrownBy(() -> service.fileApply(request))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_001);
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

    private HeartbeatRequest signedHeartbeat(String instanceId, String serial) {
        String canonical = instanceId + serial;
        String signature = CommunicationSignature.sign(canonical, privateKey);
        return new HeartbeatRequest(instanceId, serial, 4, 8192, signature);
    }

    }