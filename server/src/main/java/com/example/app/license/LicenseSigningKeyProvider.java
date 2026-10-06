package com.example.app.license;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 授权签发私钥提供者（IAS_AUTH_FILE_APPLY 用）。
 *
 * <p>local/site 模式授权文件申请（req-37）需要服务端用签发私钥对生成的授权文件签名。
 * 私钥优先从 JVM 系统属性 {@code -Dlicense.company.private.key} 读取（PKCS8 Base64），
 * 未设置则使用内置默认私钥（与 {@link DefaultKeys#SERVER_PUBLIC_KEY} 对应）。</p>
 */
@Component
public class LicenseSigningKeyProvider {

    /** 内置默认签发私钥（PKCS8 Base64），与 DefaultKeys.SERVER_PUBLIC_KEY 对应。 */
    public static final String DEFAULT_PRIVATE_KEY =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDyevPv7QtV0hwRHZ6nH9hf5EX6gDXljRNUnj22Fodc+QxxCtC9ZUByg8F7VCBnTx1CyAie2zAXlZmmZ10nxsPDNoLlzCFx6zjYSzMfbcXIuYRsum/qk1a2NJma7qYsJV3UiEK8kxwpwmqmqkUrBsawfPb/rUsg3k/LULpJpU/2Fb+UeN28DgwIptrlgkZl32Exg9l5zc0bmAU6Px3t22HLPUxAfny0m7AW8Dj8aHkmPW4PsWem1Lmute+15aFH3kISLSATXtB8Ufs80bRX7lqZ4gNfUteCB0T0EuMageXtYWiyzRMF9tVLBuHZMFOGiVcsUUIcvOpR5xHpVMNYfITtAgMBAAECggEAHvLLzmRQ6A6TKn6/UHwOConWLKzPNDyXVQYwFjI162CWJ+9wyQZH/YBiN4gl7UmkZcZQ41r9KRAdC6dxtQaiLHj5UrSFKEcU0EvOJfAa0w2/9dNYlCC4wTS2zkGGJWhM242GBw+NA/9zy/s7giGxRmFiHSv/Gh4wd7YWsMOlZbZioXjmvCUVLiu+FZHGupZQIvBnNh57EjQfUhQn4/qwIL59WSFsMPXJ7OXM46GpW/F9ugd+X38H9CCjSHoUyEBXn0n+eF5SLqZ3MQsgBsbBowXM79cCpSQJdaXkjgl3BjDHb+PByMZBWtFEC+td47aBA/8RD6xclW51N5yn7QJLOQKBgQD8uywqIEUDcGhuwZ/Wwum0O/+xn1XS7xHPwU29E0XV7X6oFGlPj6ijoWxDwhIEGGNIf6iJ/eXvlF8/KOHtPOhyTPEh33tLg1OzQN4Qd38zpHeTzooxRBwuyzsGyhMLqfQUPNSipvjbEHjWdrXlItZc5R4GC7N/X41LgsROoGstDwKBgQD1ndafF3SF/ufDCdVzUYTIUoSueoq2Ov6VGLI7KjNSWKfT3cWQJlZis/+rZ+GjdpjTVOjMzAD8UhplRPgyt3S5UVXFCsB9k8snyE0jEuNQ2nW4Va1VvwzbJHnwJfkiTCprJjKo1MI2WENxq752K73E40hhgRv6FQEfj5Qpv0imQwKBgQDw7kqrGjpQjf+w3ntPVF9RSAV5Qmmh/fEf+qrufgoMaC1fdJ30kAJ3g7LfYYqlRs6XVcPJUTe7ztmCL6o57N+332vdG9zBX0AS2dsZHaIxDDBGwxZw4vpvDaWG0muXqfyasmcPbFg/FpPr+B1thGmRA4c8wjjrS586hj44qquhtwKBgHltXv2oCuNz8gBqjCxG0O54XUAlNQ6Ce/oaC4NUiarcSZxkt5dcXLjPZEbJRLQbndJc1/rnoFUeAg6u2kW4jOxRWaXgeumad4VjcT81x0vhtPE86kTJ/wEYN3CSVWivXwk6CTGOfbWATVeVIMQrO9Zqxw3tVnd8iuGfihBZkQEhAoGAJETkOsle9Xink6dUYYsa/j0BoC1498K9mvS7olFswnXoWVdCPpXiqZoWmbBIvvRIfti6ouLiFzdBXm0jJkWc64/yHfmhz5mwhbWbZ421OKzgNo545EW5/gHMEGx0YZWVTJ8NUi/qNTMP4Zpi5ebetVg1ynlC2XRsnU6B2crDdNg=";

    private final PrivateKey privateKey;

    public LicenseSigningKeyProvider(@Value("${license.company.private.key:}") String privateKeyPem) {
        this.privateKey = loadPrivateKey(privateKeyPem);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    private PrivateKey loadPrivateKey(String pem) {
        try {
            String source = (pem != null && !pem.isBlank()) ? pem : DEFAULT_PRIVATE_KEY;
            return parsePrivateKey(source);
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.SYS_001, "授权签发私钥加载失败", e);
        }
    }

    private PrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
}