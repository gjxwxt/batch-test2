package com.example.app.license;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 通信密钥对提供者（req-12 / IAS_AUTH_KEYPAIR_INIT）。
 *
 * <p>通信密钥对（O6 裁决）与服务端「授权签名密钥对」互不复用：</p>
 * <ul>
 *   <li><b>私钥</b>：服务端首次启动自动生成，Base64(X.509/PKCS#8) 持久化，供签名注册/心跳请求与公钥分发响应。</li>
 *   <li><b>公钥</b>：通过 {@code GET /api/v1/license/public-key} 分发（req-13 / IAS_AUTH_PUBLIC_KEY）。</li>
 * </ul>
 *
 * <p>算法：RSA-2048（infra:signature 冻结契约）。</p>
 */
@Component
public class CommunicationKeyProvider {

    /** 通信密钥对算法。 */
    public static final String KEY_ALGORITHM = "RSA";
    /** 通信密钥对密钥长度。 */
    public static final int KEY_SIZE = 2048;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    /**
     * Spring 默认构造：首次启动生成新通信密钥对。
     *
     * <p>持久化私钥加载由 {@link #CommunicationKeyProvider(String)} 提供，供测试与后续
     * 配置注入使用（当前工作包未接入持久化存储，启动即生成新密钥对）。</p>
     */
    public CommunicationKeyProvider() {
        KeyPair generated = generateKeyPair();
        this.privateKey = generated.getPrivate();
        this.publicKey = generated.getPublic();
    }

    /**
     * 构造通信密钥对。
     *
     * <p>优先使用已持久化的私钥（Base64 PKCS#8），否则首次启动生成新密钥对。</p>
     *
     * @param persistedPrivateKey 已持久化的私钥（Base64 PKCS#8），可为空表示首次启动
     */
    public CommunicationKeyProvider(String persistedPrivateKey) {
        if (persistedPrivateKey != null && !persistedPrivateKey.isBlank()) {
            KeyPair loaded = loadKeyPair(persistedPrivateKey);
            this.privateKey = loaded.getPrivate();
            this.publicKey = loaded.getPublic();
        } else {
            KeyPair generated = generateKeyPair();
            this.privateKey = generated.getPrivate();
            this.publicKey = generated.getPublic();
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * 返回公钥的 Base64（X.509 SubjectPublicKeyInfo）编码，用于接口分发。
     */
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 返回私钥的 Base64（PKCS#8）编码，用于持久化。
     */
    public String getPrivateKeyBase64() {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.SYS_001, "通信密钥对生成失败", e);
        }
    }

    private KeyPair loadKeyPair(String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            PublicKey publicKey = derivePublicKey(privateKey);
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.SYS_001, "通信密钥对加载失败", e);
        }
    }

    private PublicKey derivePublicKey(PrivateKey privateKey) throws Exception {
        // 从私钥推导公钥（RSA 私钥 PKCS#8 含模数与公钥指数）
        java.security.interfaces.RSAPrivateCrtKey rsaPrivate =
                (java.security.interfaces.RSAPrivateCrtKey) privateKey;
        java.security.spec.RSAPublicKeySpec publicKeySpec =
                new java.security.spec.RSAPublicKeySpec(rsaPrivate.getModulus(), rsaPrivate.getPublicExponent());
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(publicKeySpec);
    }
}