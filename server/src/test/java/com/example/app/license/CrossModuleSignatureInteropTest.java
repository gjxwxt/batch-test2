package com.example.app.license;

import com.example.app.exception.LicenseException;
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
 * 跨模块签名互操作测试（CRITICAL-1 回归）。
 *
 * <p>验证服务端验签（LicenseSignature）与签发工具（tools）使用同一 canonical 格式：
 * {@code InforSuiteAuth2026_<k>=<v>&<k>=<v>...}（空值省略）。签发工具签名的授权文件
 * 必须能被服务端导入与三层防篡改校验通过（req-6/7/33 全链路）。</p>
 */
class CrossModuleSignatureInteropTest {

    private static final String SALT_PREFIX = "InforSuiteAuth2026_";
    private static final String[] CANONICAL_ORDER = {
            "component", "version", "licensee", "mode", "formal", "expiration",
            "userinfor", "proname", "serial", "center-required",
            "max-instances", "max-cpus", "max-memory"
    };

    @Test
    @DisplayName("tools 格式 canonical 签名的授权可被服务端验签通过")
    void toolsSignedLicenseVerifiesOnServer() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = validFields();

        // 使用与 tools/CanonicalStringBuilder 相同的 canonical 格式签名
        String canonical = toolsCanonicalString(fields);
        String signature = sign(keyPair.getPrivate(), canonical);
        fields.put("signature", signature);

        // 服务端验签（LicenseSignature.verifySignature）
        LicenseSignature.verifySignature(fields, keyPair.getPublic());
    }

    @Test
    @DisplayName("tools 格式 canonical 签名的授权被篡改后服务端验签失败")
    void tamperedToolsSignedLicenseFailsOnServer() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = validFields();

        String canonical = toolsCanonicalString(fields);
        String signature = sign(keyPair.getPrivate(), canonical);
        fields.put("signature", signature);

        // 篡改 canonical 字段
        fields.put("version", "2.0");

        assertThatThrownBy(() -> LicenseSignature.verifySignature(fields, keyPair.getPublic()))
                .isInstanceOf(LicenseException.class);
    }

    @Test
    @DisplayName("服务端 buildCanonicalString 与 tools 格式一致")
    void serverCanonicalMatchesToolsFormat() {
        Map<String, String> fields = validFields();

        String serverCanonical = LicenseSignature.buildCanonicalString(fields);
        String toolsCanonical = toolsCanonicalString(fields);

        assertThat(serverCanonical).isEqualTo(toolsCanonical);
    }

    /**
     * 与 tools/CanonicalStringBuilder 完全一致的 canonical 构建（冻结契约 §4）。
     */
    private String toolsCanonicalString(Map<String, String> fields) {
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

    private Map<String, String> validFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", "Server");
        fields.put("version", "1.0");
        fields.put("licensee", "InforSuite Auth Center");
        fields.put("mode", "center");
        fields.put("formal", "true");
        fields.put("expiration", "never");
        fields.put("userinfor", "auth-center");
        fields.put("proname", "AS");
        fields.put("serial", "auth-center-local-001");
        fields.put("center-required", "true");
        fields.put("max-instances", "100");
        fields.put("max-cpus", "64");
        fields.put("max-memory", "131072");
        return fields;
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}