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
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA185Vp5tNgXUjkd497g0OBDkRq8wk8UqGoHg7Lhykxb4IKmXrXW/SgN1YWMYCVygkDBjN02viiY0os+WOlzLDUXb4vywJAjGAr3PeyKBQTYzPuTHpYsXqPmDPHYAsc6HJ7xYZEl2mgGq7NDoKptlr+0GKAyxWBw5H6pudveYz/tHqXGXQrajerhrY17vbWPrbDGLM+OHbKv7QhOPRKuYI9+KGKO8q5eQ2rfBLyiz4sbQx7rg1TszE1NNUWlNmVlT354jkZc4kowE8b6vhM4zaOowUUu9NvoSjrQxx37mgF2QTtuP7BMPzT5t0utvLbnXLJuuv5FSEZr+luzYG9zcl8QIDAQAB";
}