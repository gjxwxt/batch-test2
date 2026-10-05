package com.example.app.license;

import com.example.app.exception.LicenseException;
import com.example.app.exception.ErrorCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 授权文件签名工具（infra:signature 冻结契约）。
 *
 * <p>负责授权文件 XML 解析、canonical 字符串构建与 RSA-2048 + SHA256withRSA 验签。
 * 供授权导入（IAS_AUTH_IMPORT）与三层防篡改校验（IAS_AUTH_TAMPER_CHECK）复用。</p>
 */
public final class LicenseSignature {

    /** 盐值前缀（冻结契约）。 */
    public static final String SALT_PREFIX = "InforSuiteAuth2026_";

    /** canonical 字段固定顺序（冻结契约）。 */
    public static final String[] CANONICAL_FIELD_ORDER = {
            "component", "version", "licensee", "mode", "formal", "expiration",
            "userinfor", "proname", "serial", "center-required",
            "max-instances", "max-cpus", "max-memory"
    };

    private LicenseSignature() {
    }

    /**
     * 解析授权文件 XML 为字段映射（含 signature 节点）。
     *
     * @throws LicenseException 当 XML 解析失败时（LICENSE_002）
     */
    public static Map<String, String> parseLicenseXml(String licenseContent) {
        if (licenseContent == null || licenseContent.isBlank()) {
            throw new LicenseException(ErrorCode.LICENSE_002, "授权文件不存在或内容为空");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(licenseContent)));

            Map<String, String> fields = new LinkedHashMap<>();
            for (String field : CANONICAL_FIELD_ORDER) {
                fields.put(field, textOf(doc, field));
            }
            fields.put("signature", textOf(doc, "signature"));
            return fields;
        } catch (LicenseException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.LICENSE_002, "授权文件解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 依据 canonical 字段顺序构建签名原文（盐值前缀 + 各字段值拼接）。
     */
    public static String buildCanonicalString(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder(SALT_PREFIX);
        for (String field : CANONICAL_FIELD_ORDER) {
            sb.append(fields.getOrDefault(field, ""));
        }
        return sb.toString();
    }

    /**
     * 校验授权文件数字签名（RSA-2048 + SHA256withRSA）。
     *
     * @throws LicenseException 当签名缺失或验证失败时（LICENSE_002）
     */
    public static void verifySignature(Map<String, String> fields, PublicKey publicKey) {
        String signatureB64 = fields.getOrDefault("signature", "");
        if (signatureB64 == null || signatureB64.isBlank()) {
            throw new LicenseException(ErrorCode.LICENSE_002, "授权文件缺少签名节点");
        }
        String canonical = buildCanonicalString(fields);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getDecoder().decode(signatureB64);
            if (!signature.verify(sigBytes)) {
                throw new LicenseException(ErrorCode.LICENSE_002, "授权签名验证失败");
            }
        } catch (LicenseException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.LICENSE_002, "授权签名验证失败：" + e.getMessage(), e);
        }
    }

    private static String textOf(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        Element element = (Element) nodes.item(0);
        return element.getTextContent() == null ? "" : element.getTextContent().trim();
    }
}