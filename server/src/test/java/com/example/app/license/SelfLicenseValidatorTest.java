package com.example.app.license;

import com.example.app.exception.LicenseValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SelfLicenseValidator 单元测试（TDD）。
 * 覆盖服务端启动自检（IAS_AUTH_SELF_CHECK）的 5 个步骤：
 * 1. 文件存在性  2. 数字签名验证  3. 组件标识匹配  4. 产品标识匹配  5. 有效期校验。
 */
class SelfLicenseValidatorTest {

    private static final String SALT_PREFIX = "InforSuiteAuth2026_";
    private static final String[] CANONICAL_ORDER = {
            "component", "version", "licensee", "mode", "formal", "expiration",
            "userinfor", "proname", "serial", "center-required",
            "max-instances", "max-cpus", "max-memory"
    };

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
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

    private String buildLicenseXml(Map<String, String> fields, String signature) {
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

    @Test
    @DisplayName("有效授权文件通过全部自检步骤")
    void shouldPassAllChecksForValidLicense() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = validFields();
        String canonical = canonicalString(fields);
        String signature = sign(keyPair.getPrivate(), canonical);
        String xml = buildLicenseXml(fields, signature);

        SelfLicenseValidator validator = new SelfLicenseValidator(
                xml, keyPair.getPublic(), "Server", "AS");

        assertThat(validator.validate()).isTrue();
    }

    @Test
    @DisplayName("签名被篡改时验证失败")
    void shouldFailWhenSignatureTampered() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = validFields();
        String canonical = canonicalString(fields);
        String signature = sign(keyPair.getPrivate(), canonical);
        // 篡改 canonical 字段
        fields.put("version", "2.0");
        String xml = buildLicenseXml(fields, signature);

        SelfLicenseValidator validator = new SelfLicenseValidator(
                xml, keyPair.getPublic(), "Server", "AS");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(LicenseValidationException.class)
                .hasMessageContaining("签名");
    }

    @Test
    @DisplayName("组件标识不匹配时验证失败")
    void shouldFailWhenComponentMismatch() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = validFields();
        String canonical = canonicalString(fields);
        String signature = sign(keyPair.getPrivate(), canonical);
        String xml = buildLicenseXml(fields, signature);

        SelfLicenseValidator validator = new SelfLicenseValidator(
                xml, keyPair.getPublic(), "WrongComponent", "AS");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(LicenseValidationException.class)
                .hasMessageContaining("组件");
    }

    @Test
    @DisplayName("产品标识不匹配时验证失败")
    void shouldFailWhenProductMismatch() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = validFields();
        String canonical = canonicalString(fields);
        String signature = sign(keyPair.getPrivate(), canonical);
        String xml = buildLicenseXml(fields, signature);

        SelfLicenseValidator validator = new SelfLicenseValidator(
                xml, keyPair.getPublic(), "Server", "WrongProduct");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(LicenseValidationException.class)
                .hasMessageContaining("产品");
    }

    @Test
    @DisplayName("授权已过期时验证失败")
    void shouldFailWhenExpired() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = validFields();
        fields.put("expiration", "2020-01-01");
        String canonical = canonicalString(fields);
        String signature = sign(keyPair.getPrivate(), canonical);
        String xml = buildLicenseXml(fields, signature);

        SelfLicenseValidator validator = new SelfLicenseValidator(
                xml, keyPair.getPublic(), "Server", "AS");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(LicenseValidationException.class)
                .hasMessageContaining("过期");
    }

    @Test
    @DisplayName("授权文件为空时验证失败")
    void shouldFailWhenLicenseContentEmpty() throws Exception {
        KeyPair keyPair = generateKeyPair();

        SelfLicenseValidator validator = new SelfLicenseValidator(
                "", keyPair.getPublic(), "Server", "AS");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(LicenseValidationException.class)
                .hasMessageContaining("文件");
    }
}