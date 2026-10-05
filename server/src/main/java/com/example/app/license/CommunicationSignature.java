package com.example.app.license;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 通信签名工具（infra:signature 冻结契约）。
 *
 * <p>使用通信密钥对（O6 裁决）对注册/心跳请求与公钥分发响应做 RSA-2048 + SHA256withRSA 签名/验签。
 * 与授权文件签发密钥对互不复用。</p>
 */
public final class CommunicationSignature {

    /** 签名算法（冻结契约）。 */
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private CommunicationSignature() {
    }

    /**
     * 对请求原文签名（服务端私钥）。
     *
     * @param canonical 待签名原文（canonical 拼接字符串）
     * @param privateKey 通信私钥
     * @return Base64 编码的签名值
     */
    public static String sign(String canonical, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.SYS_001, "通信签名失败", e);
        }
    }

    /**
     * 校验请求签名（服务端公钥）。
     *
     * @param canonical 待验签原文
     * @param signatureBase64 客户端提交的 Base64 签名值
     * @param publicKey 通信公钥
     * @throws LicenseException 当签名缺失或验证失败时（LICENSE_002）
     */
    public static void verify(String canonical, String signatureBase64, PublicKey publicKey) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            throw new LicenseException(ErrorCode.LICENSE_002, "请求缺少签名");
        }
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
            if (!signature.verify(sigBytes)) {
                throw new LicenseException(ErrorCode.LICENSE_002, "请求签名验证失败");
            }
        } catch (LicenseException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.LICENSE_002, "请求签名验证失败：" + e.getMessage(), e);
        }
    }
}