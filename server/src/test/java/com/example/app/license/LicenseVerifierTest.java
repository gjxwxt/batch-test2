package com.example.app.license;

import com.example.app.model.License;
import com.example.app.model.LicenseVerifyResponse;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 三层防篡改校验（IAS_AUTH_TAMPER_CHECK）单元测试（TDD）。
 *
 * <p>覆盖三层：SIGNATURE（数字签名）/ DB_STATE（数据库状态）/ FILE_INTEGRITY（存储文件一致性）。</p>
 */
class LicenseVerifierTest {

    private static final String SALT_PREFIX = "InforSuiteAuth2026_";
    private static final String[] CANONICAL_ORDER = {
            "component", "version", "licensee", "mode", "formal", "expiration",
            "userinfor", "proname", "serial", "center-required",
            "max-instances", "max-cpus", "max-memory"
    };

    private KeyPair keyPair;
    private LicenseVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = generateKeyPair();
        LicensePublicKeyProvider provider = new LicensePublicKeyProvider(encodePublicKey(keyPair.getPublic()));
        verifier = new LicenseVerifier(provider);
    }

    @Test
    @DisplayName("三层全部通过：签名有效、状态正常、文件一致")
    void shouldPassAllThreeLayersForValidLicense() throws Exception {
        Map<String, String> fields = validFields();
        String xml = signedXml(fields, keyPair.getPrivate());
        License license = licenseFromFields(fields, xml, License.STATUS_ACTIVE);

        LicenseVerifyResponse response = verifier.verify(license);

        assertThat(response.verified()).isTrue();
        assertThat(response.layers()).hasSize(3);
        assertThat(response.layers()).allMatch(LicenseVerifyResponse.LayerResult::passed);
    }

    @Test
    @DisplayName("Layer1：签名被篡改时校验失败")
    void shouldFailSignatureLayerWhenTampered() throws Exception {
        Map<String, String> fields = validFields();
        String xml = signedXml(fields, keyPair.getPrivate());
        // 篡改 canonical 字段（version），签名不再匹配
        fields.put("version", "2.0");
        String tamperedXml = buildXml(fields, fields.get("signature"));
        License license = licenseFromFields(fields, tamperedXml, License.STATUS_ACTIVE);

        LicenseVerifyResponse response = verifier.verify(license);

        assertThat(response.verified()).isFalse();
        assertThat(layer(response, "SIGNATURE").passed()).isFalse();
    }

    @Test
    @DisplayName("Layer2：授权被禁用时校验失败")
    void shouldFailDbStateLayerWhenDisabled() throws Exception {
        Map<String, String> fields = validFields();
        String xml = signedXml(fields, keyPair.getPrivate());
        License license = licenseFromFields(fields, xml, License.STATUS_DISABLED);

        LicenseVerifyResponse response = verifier.verify(license);

        assertThat(response.verified()).isFalse();
        assertThat(layer(response, "DB_STATE").passed()).isFalse();
    }

    @Test
    @DisplayName("Layer2：授权已过期时校验失败")
    void shouldFailDbStateLayerWhenExpired() throws Exception {
        Map<String, String> fields = validFields();
        fields.put("expiration", "2020-01-01");
        String xml = signedXml(fields, keyPair.getPrivate());
        License license = licenseFromFields(fields, xml, License.STATUS_ACTIVE);

        LicenseVerifyResponse response = verifier.verify(license);

        assertThat(response.verified()).isFalse();
        assertThat(layer(response, "DB_STATE").passed()).isFalse();
    }

    @Test
    @DisplayName("Layer3：数据库记录与文件字段不一致时校验失败")
    void shouldFailFileIntegrityLayerWhenDbMismatch() throws Exception {
        Map<String, String> fields = validFields();
        String xml = signedXml(fields, keyPair.getPrivate());
        // 数据库记录 proname 与文件不一致（模拟库表被篡改）
        License license = new License(
                1L, fields.get("serial"), null, "WRONG_PRONAME",
                fields.get("component"), fields.get("version"), fields.get("licensee"),
                fields.get("mode"), fields.get("formal"), fields.get("expiration"),
                fields.get("userinfor"), 10, 8, 16384,
                0, 10, 0, 0, xml, License.STATUS_ACTIVE, "FILE",
                Instant.now(), Instant.now());

        LicenseVerifyResponse response = verifier.verify(license);

        assertThat(response.verified()).isFalse();
        assertThat(layer(response, "FILE_INTEGRITY").passed()).isFalse();
    }

    // ---- helpers ----

    private LicenseVerifyResponse.LayerResult layer(LicenseVerifyResponse response, String name) {
        return response.layers().stream()
                .filter(l -> l.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private License licenseFromFields(Map<String, String> fields, String xml, String status) {
        return new License(
                1L, fields.get("serial"), null, fields.get("proname"),
                fields.get("component"), fields.get("version"), fields.get("licensee"),
                fields.get("mode"), fields.get("formal"), fields.get("expiration"),
                fields.get("userinfor"), 10, 8, 16384,
                0, 10, 0, 0, xml, status, "FILE",
                Instant.now(), Instant.now());
    }

    private Map<String, String> validFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", "Server");
        fields.put("version", "1.0");
        fields.put("licensee", "Test Corp");
        fields.put("mode", "center");
        fields.put("formal", "true");
        fields.put("expiration", "never");
        fields.put("userinfor", "test-user");
        fields.put("proname", "AS");
        fields.put("serial", "test-serial-001");
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
        for (String key : CANONICAL_ORDER) {
            sb.append(fields.getOrDefault(key, ""));
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