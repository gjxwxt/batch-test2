package com.example.app.service;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.license.LicensePublicKeyProvider;
import com.example.app.license.LicenseVerifier;
import com.example.app.model.License;
import com.example.app.model.LicenseImportRequest;
import com.example.app.model.LicenseSummary;
import com.example.app.model.LicenseVerifyResponse;
import com.example.app.repository.AuditLogRepository;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.LicenseRepository;
import com.example.app.service.impl.AuditServiceImpl;
import com.example.app.service.impl.LicenseAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 授权管理服务（wp-3）单元测试（TDD）。
 *
 * <p>覆盖导入 / 列表 / 详情 / 防篡改校验 / 删除 / 禁用，以及 LICENSE_001/002/004/005/006 错误码。</p>
 */
class LicenseAdminServiceTest {

    private static final String SALT_PREFIX = "InforSuiteAuth2026_";
    private static final String[] CANONICAL_ORDER = {
            "component", "version", "licensee", "mode", "formal", "expiration",
            "userinfor", "proname", "serial", "center-required",
            "max-instances", "max-cpus", "max-memory"
    };

    private LicenseRepository repository;
    private LicenseAdminService service;
    private KeyPair keyPair;
    private LicensePublicKeyProvider provider;
    private LicenseVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        repository = new LicenseRepository();
        keyPair = generateKeyPair();
        provider = new LicensePublicKeyProvider(encodePublicKey(keyPair.getPublic()));
        verifier = new LicenseVerifier(provider);
        AuditServiceImpl auditService = new AuditServiceImpl(new AuditLogRepository());
        service = new LicenseAdminServiceImpl(repository, provider, verifier, auditService,
                new InstanceRepository());
    }

    @Test
    @DisplayName("导入有效授权成功，状态为 ACTIVE")
    void shouldImportValidLicense() throws Exception {
        Map<String, String> fields = validFields("serial-import-001");
        String xml = signedXml(fields, keyPair.getPrivate());

        License imported = service.importLicense(new LicenseImportRequest(xml, "My License"));

        assertThat(imported.id()).isNotNull();
        assertThat(imported.serial()).isEqualTo("serial-import-001");
        assertThat(imported.status()).isEqualTo(License.STATUS_ACTIVE);
        assertThat(imported.proname()).isEqualTo("AS");
        assertThat(imported.licenseMode()).isEqualTo("center");
        assertThat(imported.maxInstances()).isEqualTo(10);
        assertThat(imported.remainingInstances()).isEqualTo(10);
    }

    @Test
    @DisplayName("导入签名被篡改的授权抛出 LICENSE_002")
    void shouldRejectTamperedLicenseOnImport() throws Exception {
        Map<String, String> fields = validFields("serial-tampered");
        String xml = signedXml(fields, keyPair.getPrivate());
        // 篡改 version 字段
        fields.put("version", "9.9");
        String tamperedXml = buildXml(fields, fields.get("signature"));

        assertThatThrownBy(() -> service.importLicense(new LicenseImportRequest(tamperedXml, null)))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_002);
    }

    @Test
    @DisplayName("重复导入相同序列号抛出 LICENSE_005")
    void shouldRejectDuplicateSerialOnImport() throws Exception {
        Map<String, String> fields = validFields("serial-dup");
        String xml = signedXml(fields, keyPair.getPrivate());
        service.importLicense(new LicenseImportRequest(xml, null));

        assertThatThrownBy(() -> service.importLicense(new LicenseImportRequest(xml, null)))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_005);
    }

    @Test
    @DisplayName("导入已过期授权抛出 LICENSE_004")
    void shouldRejectExpiredLicenseOnImport() throws Exception {
        Map<String, String> fields = validFields("serial-expired");
        fields.put("expiration", "2020-01-01");
        String xml = signedXml(fields, keyPair.getPrivate());

        assertThatThrownBy(() -> service.importLicense(new LicenseImportRequest(xml, null)))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_004);
    }

    @Test
    @DisplayName("查询不存在的授权抛出 LICENSE_001")
    void shouldThrowLicenseNotFound() {
        assertThatThrownBy(() -> service.getLicense(999L))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_001);
    }

    @Test
    @DisplayName("列表返回导入的授权概要")
    void shouldListLicenses() throws Exception {
        Map<String, String> fields = validFields("serial-list-001");
        String xml = signedXml(fields, keyPair.getPrivate());
        service.importLicense(new LicenseImportRequest(xml, "List License"));

        List<LicenseSummary> summaries = service.listLicenses(null, null, null);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).serial()).isEqualTo("serial-list-001");
        assertThat(summaries.get(0).status()).isEqualTo(License.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("AUTH-023 授权列表支持状态过滤与分页")
    void shouldFilterAndPaginateLicenses() throws Exception {
        for (int i = 0; i < 5; i++) {
            Map<String, String> fields = validFields("serial-page-" + i);
            String xml = signedXml(fields, keyPair.getPrivate());
            service.importLicense(new LicenseImportRequest(xml, null));
        }
        // 禁用一条，制造状态差异
        License first = service.getLicense(1L);
        service.disableLicense(first.id());

        // 状态过滤：仅 ACTIVE
        List<LicenseSummary> active = service.listLicenses("ACTIVE", null, null);
        assertThat(active).hasSize(4);

        // 分页：page=1, size=2 → 2 条
        List<LicenseSummary> page1 = service.listLicenses(null, 1, 2);
        assertThat(page1).hasSize(2);

        // 总数
        assertThat(service.countLicenses(null)).isEqualTo(5);
        assertThat(service.countLicenses("ACTIVE")).isEqualTo(4);
    }

    @Test
    @DisplayName("防篡改校验通过返回 verified=true")
    void shouldVerifyLicenseSuccessfully() throws Exception {
        Map<String, String> fields = validFields("serial-verify-001");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));

        LicenseVerifyResponse response = service.verifyLicense(imported.id());

        assertThat(response.verified()).isTrue();
        assertThat(response.serial()).isEqualTo("serial-verify-001");
    }

    @Test
    @DisplayName("删除存在在线实例的授权抛出 LICENSE_006")
    void shouldRejectDeleteWhenOnlineInstances() throws Exception {
        Map<String, String> fields = validFields("serial-delete-online");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));
        // 模拟存在在线实例
        License withInstances = new License(
                imported.id(), imported.serial(), imported.licenseName(), imported.proname(),
                imported.component(), imported.version(), imported.licensee(), imported.licenseMode(),
                imported.formal(), imported.expiration(), imported.userinfor(), imported.maxInstances(),
                imported.maxCpus(), imported.maxMemory(), 3, 7, 0, 0, imported.bxbFile(),
                imported.status(), imported.source(), imported.createTime(), imported.updateTime());
        repository.save(withInstances);

        assertThatThrownBy(() -> service.deleteLicense(imported.id()))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_006);
    }

    @Test
    @DisplayName("删除无在线实例的授权成功")
    void shouldDeleteLicenseWhenNoOnlineInstances() throws Exception {
        Map<String, String> fields = validFields("serial-delete-ok");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));

        service.deleteLicense(imported.id());

        assertThatThrownBy(() -> service.getLicense(imported.id()))
                .isInstanceOf(LicenseException.class)
                .extracting(e -> ((LicenseException) e).getErrorCode())
                .isEqualTo(ErrorCode.LICENSE_001);
    }

    @Test
    @DisplayName("禁用授权后状态为 DISABLED")
    void shouldDisableLicense() throws Exception {
        Map<String, String> fields = validFields("serial-disable");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));

        License disabled = service.disableLicense(imported.id());

        assertThat(disabled.status()).isEqualTo(License.STATUS_DISABLED);
    }

    // ---- AUTH-016/017/018/019 防篡改自动修复/禁用/警告（CRITICAL-2）----

    @Test
    @DisplayName("AUTH-016 签名完好但字段被 DBA 篡改时自动修复数据库字段并保持 ACTIVE")
    void shouldAutoRepairTamperedFields() throws Exception {
        Map<String, String> fields = validFields("serial-tamper-016");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));

        // 模拟 DBA 篡改 max_instances 与 expiration
        License tampered = new License(
                imported.id(), imported.serial(), imported.licenseName(), imported.proname(),
                imported.component(), imported.version(), imported.licensee(), imported.licenseMode(),
                imported.formal(), "2099-01-01", imported.userinfor(), 999, imported.maxCpus(),
                imported.maxMemory(), imported.usedInstances(), imported.remainingInstances(),
                imported.usedCpus(), imported.usedMemory(), imported.bxbFile(), imported.status(),
                imported.source(), imported.createTime(), imported.updateTime());
        repository.save(tampered);

        LicenseVerifyResponse response = service.verifyLicense(imported.id());

        assertThat(response.verified()).isTrue();
        License repaired = service.getLicense(imported.id());
        assertThat(repaired.maxInstances()).isEqualTo(10);
        assertThat(repaired.expiration()).isEqualTo("never");
        assertThat(repaired.status()).isEqualTo(License.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("AUTH-017 签名破坏时授权被标记 DISABLED（不可逆）")
    void shouldDisableLicenseOnSignatureBreak() throws Exception {
        Map<String, String> fields = validFields("serial-tamper-017");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));

        // 替换授权文件破坏签名
        String tamperedXml = xml.replace("Test Corp", "Evil Corp");
        License tampered = new License(
                imported.id(), imported.serial(), imported.licenseName(), imported.proname(),
                imported.component(), imported.version(), imported.licensee(), imported.licenseMode(),
                imported.formal(), imported.expiration(), imported.userinfor(), imported.maxInstances(),
                imported.maxCpus(), imported.maxMemory(), imported.usedInstances(),
                imported.remainingInstances(), imported.usedCpus(), imported.usedMemory(),
                tamperedXml, imported.status(), imported.source(), imported.createTime(),
                imported.updateTime());
        repository.save(tampered);

        LicenseVerifyResponse response = service.verifyLicense(imported.id());

        assertThat(response.verified()).isFalse();
        License after = service.getLicense(imported.id());
        assertThat(after.status()).isEqualTo(License.STATUS_DISABLED);
    }

    @Test
    @DisplayName("AUTH-018 配额守恒被破坏时自动修复 remaining=max-used")
    void shouldRepairQuotaConservation() throws Exception {
        Map<String, String> fields = validFields("serial-tamper-018");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));

        // 模拟 DBA 修改 used_instances 破坏守恒（used=3, remaining=7, max=10 → 3+7=10 正常；
        // 改为 used=5, remaining=7 → 5+7=12≠10 破坏）
        License tampered = new License(
                imported.id(), imported.serial(), imported.licenseName(), imported.proname(),
                imported.component(), imported.version(), imported.licensee(), imported.licenseMode(),
                imported.formal(), imported.expiration(), imported.userinfor(), imported.maxInstances(),
                imported.maxCpus(), imported.maxMemory(), 5, 7, imported.usedCpus(),
                imported.usedMemory(), imported.bxbFile(), imported.status(), imported.source(),
                imported.createTime(), imported.updateTime());
        repository.save(tampered);

        LicenseVerifyResponse response = service.verifyLicense(imported.id());

        assertThat(response.verified()).isTrue();
        License repaired = service.getLicense(imported.id());
        assertThat(repaired.remainingInstances()).isEqualTo(5); // 10 - 5
    }

    @Test
    @DisplayName("AUTH-019 实际在线实例数大于 used_instances 时返回 WARNING")
    void shouldReturnWarningOnInstanceCountMismatch() throws Exception {
        Map<String, String> fields = validFields("serial-tamper-019");
        String xml = signedXml(fields, keyPair.getPrivate());
        License imported = service.importLicense(new LicenseImportRequest(xml, null));

        // 构造 3 个在线实例，但 used_instances=0
        InstanceRepository instanceRepo = new InstanceRepository();
        for (int i = 0; i < 3; i++) {
            instanceRepo.save(new com.example.app.model.Instance(
                    null, "inst-" + i, imported.id(), "uuid-" + i, "InforSuiteAS", "AS",
                    "1.0", "standard", "host-" + i, "10.0.0." + i, "mac-" + i, "VM",
                    2, 4096, null, com.example.app.model.Instance.STATUS_ONLINE,
                    java.time.Instant.now(), java.time.Instant.now(), null,
                    java.time.Instant.now(), java.time.Instant.now()));
        }
        // 重新构造 service 注入含实例的仓储
        AuditServiceImpl auditService = new AuditServiceImpl(new AuditLogRepository());
        LicenseAdminService svc = new LicenseAdminServiceImpl(repository, provider, verifier,
                auditService, instanceRepo);

        LicenseVerifyResponse response = svc.verifyLicense(imported.id());

        assertThat(response.verified()).isTrue();
        assertThat(response.message()).contains("WARNING");
    }

    // ---- helpers ----

    private Map<String, String> validFields(String serial) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", "Server");
        fields.put("version", "1.0");
        fields.put("licensee", "Test Corp");
        fields.put("mode", "center");
        fields.put("formal", "true");
        fields.put("expiration", "never");
        fields.put("userinfor", "test-user");
        fields.put("proname", "AS");
        fields.put("serial", serial);
        fields.put("center-required", "true");
        fields.put("max-instances", "10");
        fields.put("max-cpus", "8");
        fields.put("max-memory", "16384");
        return fields;
    }

    private String signedXml(Map<String, String> fields, PrivateKey privateKey) throws Exception {
        String canonical = canonicalString(fields);
        String signature = sign(privateKey, canonical);
        fields.put("signature", signature);
        return buildXml(fields, signature);
    }

    private String canonicalString(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder(SALT_PREFIX);
        boolean first = true;
        for (String key : CANONICAL_ORDER) {
            String value = fields.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append('&');
            }
            sb.append(key).append('=').append(value);
            first = false;
        }
        return sb.toString();
    }

    private String sign(PrivateKey privateKey, String canonical) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private String buildXml(Map<String, String> fields, String signature) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<license>\n");
        for (String key : CANONICAL_ORDER) {
            sb.append("  <").append(key).append(">")
                    .append(fields.getOrDefault(key, ""))
                    .append("</").append(key).append(">\n");
        }
        sb.append("  <signature>").append(signature).append("</signature>\n");
        sb.append("</license>\n");
        return sb.toString();
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}