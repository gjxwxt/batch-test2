package com.example.app.license;

import com.example.app.exception.LicenseValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 服务端启动自检（IAS_AUTH_SELF_CHECK）。
 *
 * <p>在 Spring 容器启动完成后执行，校验服务端自身授权文件：
 * <ol>
 *   <li>文件存在性（{@code license.self.path}，默认 classpath:test_license/auth-center-local-license.infor）</li>
 *   <li>数字签名验证（SHA256withRSA）</li>
 *   <li>组件标识匹配（{@code license.self.expected-component}，默认 Server）</li>
 *   <li>产品标识匹配（{@code license.self.expected-proname}，默认 AS）</li>
 *   <li>有效期校验（never 或日期格式，过期则失败）</li>
 * </ol>
 * 任一步失败即抛出 {@link LicenseValidationException}，终止应用启动。
 */
@Component
public class ServerSelfCheck {

    private static final Logger log = LoggerFactory.getLogger(ServerSelfCheck.class);

    private final ResourceLoader resourceLoader;
    private final String licensePath;
    private final String expectedComponent;
    private final String expectedProname;
    private final String publicKeyPem;

    public ServerSelfCheck(
            ResourceLoader resourceLoader,
            @Value("${license.self.path:classpath:test_license/auth-center-local-license.infor}") String licensePath,
            @Value("${license.self.expected-component:Server}") String expectedComponent,
            @Value("${license.self.expected-proname:AS}") String expectedProname,
            @Value("${license.company.pub.key:}") String publicKeyPem) {
        this.resourceLoader = resourceLoader;
        this.licensePath = licensePath;
        this.expectedComponent = expectedComponent;
        this.expectedProname = expectedProname;
        this.publicKeyPem = publicKeyPem;
    }

    @PostConstruct
    public void runSelfCheck() {
        log.info("服务端启动自检开始：license.self.path={}", licensePath);
        String content = loadLicenseContent();
        PublicKey publicKey = loadPublicKey();
        SelfLicenseValidator validator = new SelfLicenseValidator(
                content, publicKey, expectedComponent, expectedProname);
        validator.validate();
        log.info("服务端启动自检通过");
    }

    private String loadLicenseContent() {
        try {
            Resource resource = resourceLoader.getResource(licensePath);
            if (!resource.exists()) {
                throw new LicenseValidationException("授权文件不存在：" + licensePath);
            }
            try (InputStream in = resource.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (LicenseValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseValidationException("授权文件读取失败：" + licensePath, e);
        }
    }

    private PublicKey loadPublicKey() {
        try {
            if (publicKeyPem != null && !publicKeyPem.isBlank()) {
                return parsePublicKey(publicKeyPem);
            }
            // 未配置则使用内置默认公钥（签发密钥对公钥）
            return parsePublicKey(DefaultKeys.SERVER_PUBLIC_KEY);
        } catch (Exception e) {
            throw new LicenseValidationException("授权验签公钥加载失败", e);
        }
    }

    private PublicKey parsePublicKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}