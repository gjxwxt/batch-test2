package com.example.app.license;

import com.example.app.exception.LicenseValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * 服务端启动自检（IAS_AUTH_SELF_CHECK）。
 *
 * <p>顺序执行以下校验，任一步失败即抛出 {@link LicenseValidationException} 终止启动：
 * <ol>
 *   <li>授权文件内容存在性</li>
 *   <li>数字签名验证（RSA-2048 + SHA256withRSA）</li>
 *   <li>组件标识匹配（默认 Server，大小写不敏感）</li>
 *   <li>产品标识匹配（默认 AS）</li>
 *   <li>有效期校验（never 或日期格式，过期则失败）</li>
 * </ol>
 *
 * <p>签名 canonical 字段顺序与盐值前缀为冻结契约（infra:signature）。
 */
public class SelfLicenseValidator {

    private static final Logger log = LoggerFactory.getLogger(SelfLicenseValidator.class);

    /** 盐值前缀（冻结契约）。 */
    public static final String SALT_PREFIX = "InforSuiteAuth2026_";

    /** canonical 字段固定顺序（冻结契约）。 */
    public static final String[] CANONICAL_FIELD_ORDER = {
            "component", "version", "licensee", "mode", "formal", "expiration",
            "userinfor", "proname", "serial", "center-required",
            "max-instances", "max-cpus", "max-memory"
    };

    private static final String NEVER = "never";

    private final String licenseContent;
    private final PublicKey publicKey;
    private final String expectedComponent;
    private final String expectedProname;

    public SelfLicenseValidator(String licenseContent, PublicKey publicKey,
                                String expectedComponent, String expectedProname) {
        this.licenseContent = licenseContent;
        this.publicKey = publicKey;
        this.expectedComponent = expectedComponent;
        this.expectedProname = expectedProname;
    }

    /**
     * 执行全部自检步骤。全部通过返回 true，任一步失败抛出 {@link LicenseValidationException}。
     */
    public boolean validate() {
        // 1. 文件存在性 / 内容非空
        if (licenseContent == null || licenseContent.isBlank()) {
            throw new LicenseValidationException("授权文件不存在或内容为空");
        }

        // 解析 XML
        Document doc = parseXml(licenseContent);

        // 2. 数字签名验证
        verifySignature(doc);

        // 3. 组件标识匹配
        String component = textOf(doc, "component");
        if (expectedComponent != null && !expectedComponent.equalsIgnoreCase(component)) {
            throw new LicenseValidationException(
                    "组件标识不匹配：期望 '" + expectedComponent + "'，实际 '" + component + "'");
        }

        // 4. 产品标识匹配
        String proname = textOf(doc, "proname");
        if (expectedProname != null && !expectedProname.equalsIgnoreCase(proname)) {
            throw new LicenseValidationException(
                    "产品标识不匹配：期望 '" + expectedProname + "'，实际 '" + proname + "'");
        }

        // 5. 有效期校验
        String expiration = textOf(doc, "expiration");
        checkExpiration(expiration);

        log.info("服务端授权自检通过：component={}, proname={}, expiration={}",
                component, proname, expiration);
        return true;
    }

    private Document parseXml(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(content)));
        } catch (Exception e) {
            throw new LicenseValidationException("授权文件解析失败：" + e.getMessage(), e);
        }
    }

    private void verifySignature(Document doc) {
        String signatureB64 = textOf(doc, "signature");
        if (signatureB64 == null || signatureB64.isBlank()) {
            throw new LicenseValidationException("授权文件缺少签名节点");
        }
        String canonical = buildCanonicalString(doc);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getDecoder().decode(signatureB64);
            if (!signature.verify(sigBytes)) {
                throw new LicenseValidationException("授权签名验证失败");
            }
        } catch (LicenseValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseValidationException("授权签名验证失败：" + e.getMessage(), e);
        }
    }

    private String buildCanonicalString(Document doc) {
        StringBuilder sb = new StringBuilder(SALT_PREFIX);
        for (String field : CANONICAL_FIELD_ORDER) {
            sb.append(textOf(doc, field));
        }
        return sb.toString();
    }

    private void checkExpiration(String expiration) {
        if (expiration == null || expiration.isBlank() || NEVER.equalsIgnoreCase(expiration)) {
            return;
        }
        try {
            LocalDate expiryDate = LocalDate.parse(expiration, DateTimeFormatter.ISO_LOCAL_DATE);
            if (expiryDate.isBefore(LocalDate.now())) {
                throw new LicenseValidationException("授权已过期：" + expiration);
            }
        } catch (DateTimeParseException e) {
            throw new LicenseValidationException("授权有效期格式非法：" + expiration, e);
        }
    }

    private String textOf(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        Element element = (Element) nodes.item(0);
        return element.getTextContent() == null ? "" : element.getTextContent().trim();
    }
}