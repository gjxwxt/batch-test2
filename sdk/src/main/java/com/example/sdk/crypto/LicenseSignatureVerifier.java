package com.example.sdk.crypto;

import com.example.sdk.exception.SdkErrorCode;
import com.example.sdk.exception.SdkException;
import com.example.sdk.model.LicenseInfo;
import com.example.sdk.model.SignatureFields;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies license signatures using RSA-2048 + SHA256withRSA.
 *
 * <p>The canonical payload is built by concatenating the license fields in the
 * frozen {@link SignatureFields#CANONICAL_ORDER}, each prefixed with the salt
 * prefix {@code InforSuiteAuth2026_}. The signature is verified against that
 * payload using the issuing public key fetched from the center.</p>
 */
public final class LicenseSignatureVerifier {

    private LicenseSignatureVerifier() {
    }

    /**
     * Build the canonical signed payload for a license.
     *
     * <p>Format (frozen contract §4): {@code InforSuiteAuth2026_<field1>=<value1>&<field2>=<value2>&...}
     * — fields joined by {@code &} as {@code key=value}, empty values omitted.
     * This must match the issuing tool (tools) and the server verifier so that
     * sign → import → verify works end-to-end.</p>
     *
     * @param fields raw license field map (raw contract field names)
     * @return the canonical payload string
     */
    public static String buildCanonicalPayload(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder(SignatureFields.SALT_PREFIX);
        boolean first = true;
        for (String field : SignatureFields.CANONICAL_ORDER) {
            // The canonical field "mode" maps to the raw license-file field
            // "license_mode" (see the shared license table contract).
            String rawKey = "mode".equals(field) ? "license_mode" : field;
            String value = fields.get(rawKey);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append('&');
            }
            sb.append(field).append('=').append(value);
            first = false;
        }
        return sb.toString();
    }

    /**
     * Build the canonical payload from a {@link LicenseInfo}.
     *
     * @param license the parsed license
     * @return the canonical payload string
     */
    public static String buildCanonicalPayload(LicenseInfo license) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", nvl(license.component()));
        fields.put("version", nvl(license.version()));
        fields.put("licensee", nvl(license.licensee()));
        fields.put("license_mode", nvl(license.licenseMode()));
        fields.put("formal", nvl(license.formal()));
        fields.put("expiration", nvl(license.expiration()));
        fields.put("userinfor", nvl(license.userinfor()));
        fields.put("proname", nvl(license.proname()));
        fields.put("serial", nvl(license.serial()));
        fields.put("center-required", nvl(license.centerRequired()));
        fields.put("max-instances", nvl(license.maxInstances()));
        fields.put("max-cpus", nvl(license.maxCpus()));
        fields.put("max-memory", nvl(license.maxMemory()));
        return buildCanonicalPayload(fields);
    }

    /**
     * Verify a license signature against the issuing public key.
     *
     * @param license       the parsed license (must carry a base64 signature)
     * @param publicKeyBase64 base64-encoded DER X.509 public key
     * @return {@code true} if the signature is valid
     * @throws SdkException if the key or signature cannot be decoded
     */
    public static boolean verify(LicenseInfo license, String publicKeyBase64) {
        if (license.signature() == null || license.signature().isBlank()) {
            throw new SdkException(SdkErrorCode.LICENSE_003.code(),
                    "License carries no signature");
        }
        PublicKey publicKey = decodePublicKey(publicKeyBase64);
        String payload = buildCanonicalPayload(license);
        return verify(payload, license.signature(), publicKey);
    }

    /**
     * Verify a raw payload against a base64 signature and public key.
     *
     * @param payload         the canonical payload
     * @param signatureBase64 base64-encoded signature
     * @param publicKey       the issuing public key
     * @return {@code true} if the signature is valid
     */
    public static boolean verify(String payload, String signatureBase64, PublicKey publicKey) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            Signature verifier = Signature.getInstance(SignatureFields.SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (IllegalArgumentException e) {
            throw new SdkException(SdkErrorCode.LICENSE_003.code(),
                    "Signature is not valid base64", e);
        } catch (Exception e) {
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Failed to verify license signature: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a base64-encoded DER X.509 RSA public key.
     *
     * @param publicKeyBase64 base64-encoded key
     * @return the decoded {@link PublicKey}
     * @throws SdkException if the key cannot be decoded
     */
    public static PublicKey decodePublicKey(String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (IllegalArgumentException e) {
            throw new SdkException(SdkErrorCode.LICENSE_003.code(),
                    "Public key is not valid base64", e);
        } catch (Exception e) {
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Failed to decode public key: " + e.getMessage(), e);
        }
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private static String nvl(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }
}