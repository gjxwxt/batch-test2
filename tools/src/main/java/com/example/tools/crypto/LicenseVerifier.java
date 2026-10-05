package com.example.tools.crypto;

import com.example.tools.model.CanonicalStringBuilder;
import com.example.tools.model.LicenseData;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Verifies license signatures produced by {@link LicenseSigner} using the
 * corresponding RSA public key.
 */
public final class LicenseVerifier {

    private LicenseVerifier() {
    }

    /**
     * Verifies the Base64-encoded signature against the canonical string of
     * the given license data.
     *
     * @param data      the license data to verify
     * @param publicKey the RSA public key (signing key pair)
     * @param signature the Base64-encoded signature to check
     * @return {@code true} if the signature is valid, {@code false} otherwise
     */
    public static boolean verify(LicenseData data, PublicKey publicKey, String signature) {
        try {
            String canonical = CanonicalStringBuilder.build(data);
            Signature verifier = Signature.getInstance(LicenseSigner.ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }
}