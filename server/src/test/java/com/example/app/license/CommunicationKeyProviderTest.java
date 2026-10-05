package com.example.app.license;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通信密钥对（req-12 / IAS_AUTH_KEYPAIR_INIT）单元测试。
 *
 * <p>覆盖首次启动生成、持久化私钥加载、公钥分发编码、签名/验签闭环。</p>
 */
class CommunicationKeyProviderTest {

    @Test
    @DisplayName("首次启动生成 RSA-2048 通信密钥对")
    void shouldGenerateKeyPairOnFirstStartup() {
        CommunicationKeyProvider provider = new CommunicationKeyProvider(null);

        assertThat(provider.getPrivateKey()).isNotNull();
        assertThat(provider.getPublicKey()).isNotNull();
        assertThat(provider.getPrivateKeyBase64()).isNotBlank();
        assertThat(provider.getPublicKeyBase64()).isNotBlank();
    }

    @Test
    @DisplayName("从持久化私钥加载可还原同一公钥")
    void shouldLoadKeyPairFromPersistedPrivateKey() {
        CommunicationKeyProvider first = new CommunicationKeyProvider(null);
        String persistedPrivateKey = first.getPrivateKeyBase64();

        CommunicationKeyProvider loaded = new CommunicationKeyProvider(persistedPrivateKey);

        assertThat(loaded.getPublicKeyBase64()).isEqualTo(first.getPublicKeyBase64());
    }

    @Test
    @DisplayName("通信签名可被通信公钥验签通过")
    void shouldSignAndVerifyWithCommunicationKey() throws Exception {
        CommunicationKeyProvider provider = new CommunicationKeyProvider(null);
        PrivateKey privateKey = provider.getPrivateKey();
        PublicKey publicKey = provider.getPublicKey();

        String canonical = "serial-001client-uuid-1AS";
        String signature = CommunicationSignature.sign(canonical, privateKey);

        // 验签通过不抛异常
        CommunicationSignature.verify(canonical, signature, publicKey);
    }

    @Test
    @DisplayName("篡改原文后验签失败")
    void shouldRejectTamperedCanonical() throws Exception {
        CommunicationKeyProvider provider = new CommunicationKeyProvider(null);
        PrivateKey privateKey = provider.getPrivateKey();
        PublicKey publicKey = provider.getPublicKey();

        String canonical = "serial-001client-uuid-1AS";
        String signature = CommunicationSignature.sign(canonical, privateKey);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.app.exception.LicenseException.class,
                () -> CommunicationSignature.verify(canonical + "tampered", signature, publicKey));
    }
}