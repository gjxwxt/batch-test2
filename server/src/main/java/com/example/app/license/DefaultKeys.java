package com.example.app.license;

/**
 * 内置默认验签公钥（签发密钥对公钥）。
 *
 * <p>对应授权文件签发私钥。生产环境应通过 JVM 系统属性 {@code -Dlicense.company.pub.key} 覆盖。</p>
 */
public final class DefaultKeys {

    private DefaultKeys() {
    }

    /** 签发密钥对公钥（RSA-2048，X.509 SubjectPublicKeyInfo Base64）。 */
    public static final String SERVER_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtJ7dN4ZeWmoKraK0bA6+Ozy/1ftyYzjMfsJfqtAq+Qo7ZBeu2fzuttIqXNeBcKEGNIUUlePYKwh327E538nlVqPGkkaSQ6bYxgcGWNAuppmPEnEcYN3FhjjoyObvKzq0JwGj/aBMzl9XzH/qPOkqe60jq/JOmfYMJrjFkoi0Q9+eyeAJzlQw8uVy5xhM/UXXJfn9myPAb8A+N0Ue/JbKwwoFUJdE11sWXpVSn2oYh98onYMi/izGzP2NgGJPBfQ1VMW+V1fa72e6x1aPgdR76acT0wJOI5UvpHmj2Qf9LaWGZc4mMVfjA8xwsSMFyv+qyrkVZV4gC70dV777U1fYYwIDAQAB";
}