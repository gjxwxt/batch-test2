package com.example.app.model;

/**
 * 通信公钥分发响应（req-13 / IAS_AUTH_PUBLIC_KEY）。
 *
 * @param algorithm 密钥算法（RSA）
 * @param keySize   密钥长度（2048）
 * @param publicKey 公钥 Base64（X.509 SubjectPublicKeyInfo）
 */
public record PublicKeyResponse(
        String algorithm,
        Integer keySize,
        String publicKey
) {}