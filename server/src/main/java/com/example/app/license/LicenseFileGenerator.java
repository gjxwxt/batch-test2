package com.example.app.license;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 授权文件生成器（IAS_AUTH_FILE_APPLY 用）。
 *
 * <p>local/site 模式授权文件申请（req-37）时，服务端按冻结 canonical 字段顺序构建授权文件
 * XML，并用签发私钥签名后返回给客户端。生成的授权文件可被服务端与客户端 SDK 用签发公钥验签。</p>
 */
@Component
public class LicenseFileGenerator {

    private final LicenseSigningKeyProvider signingKeyProvider;

    public LicenseFileGenerator(LicenseSigningKeyProvider signingKeyProvider) {
        this.signingKeyProvider = signingKeyProvider;
    }

    /**
     * 生成并签名一份授权文件 XML。
     *
     * @param fields 授权字段（key 为 canonical 字段名，如 component/version/serial 等）
     * @return 带 {@code <signature>} 节点的授权文件 XML 文本
     */
    public String generate(Map<String, String> fields) {
        Map<String, String> canonicalFields = new LinkedHashMap<>();
        for (String field : LicenseSignature.CANONICAL_FIELD_ORDER) {
            String value = fields.get(field);
            if (value != null) {
                canonicalFields.put(field, value);
            }
        }

        String canonical = LicenseSignature.buildCanonicalString(canonicalFields);
        String signatureB64 = sign(canonical, signingKeyProvider.getPrivateKey());

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<license>\n");
        for (String field : LicenseSignature.CANONICAL_FIELD_ORDER) {
            String value = canonicalFields.get(field);
            if (value != null && !value.isEmpty()) {
                xml.append("  <").append(field).append(">").append(escapeXml(value))
                        .append("</").append(field).append(">\n");
            }
        }
        xml.append("  <signature>").append(signatureB64).append("</signature>\n</license>\n");
        return xml.toString();
    }

    private String sign(String canonical, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.SYS_001, "授权文件签名失败：" + e.getMessage(), e);
        }
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}