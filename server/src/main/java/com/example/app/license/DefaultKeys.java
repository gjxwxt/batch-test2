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
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8nrz7+0LVdIcER2epx/YX+RF+oA15Y0TVJ49thaHXPkMcQrQvWVAcoPBe1QgZ08dQsgIntswF5WZpmddJ8bDwzaC5cwhces42EszH23FyLmEbLpv6pNWtjSZmu6mLCVd1IhCvJMcKcJqpqpFKwbGsHz2/61LIN5Py1C6SaVP9hW/lHjdvA4MCKba5YJGZd9hMYPZec3NG5gFOj8d7dthyz1MQH58tJuwFvA4/Gh5Jj1uD7FnptS5rrXvteWhR95CEi0gE17QfFH7PNG0V+5ameIDX1LXggdE9BLjGoHl7WFoss0TBfbVSwbh2TBTholXLFFCHLzqUecR6VTDWHyE7QIDAQAB";
}