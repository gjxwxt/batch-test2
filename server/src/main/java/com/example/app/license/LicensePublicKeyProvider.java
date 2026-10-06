package com.example.app.license;

import com.example.app.exception.LicenseException;
import com.example.app.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 授权验签公钥提供者。
 *
 * <p>优先从 JVM 系统属性 {@code -Dlicense.company.pub.key} 读取（infra:signature），
 * 未设置则使用内置默认公钥（签发密钥对公钥）。</p>
 */
@Component
public class LicensePublicKeyProvider {

    private final PublicKey publicKey;

    public LicensePublicKeyProvider(@Value("${license.company.pub.key:}") String publicKeyPem) {
        this.publicKey = loadPublicKey(publicKeyPem);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PublicKey loadPublicKey(String pem) {
        try {
            String source = (pem != null && !pem.isBlank()) ? pem : DefaultKeys.SERVER_PUBLIC_KEY;
            return parsePublicKey(source);
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.SYS_001, "授权验签公钥加载失败", e);
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