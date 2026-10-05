package com.example.tools.crypto;

import com.example.tools.model.CanonicalStringBuilder;
import com.example.tools.model.LicenseData;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Signs license data using RSA-2048 + SHA256withRSA over the canonical string
 * mandated by the shared contract.
 */
public final class LicenseSigner {

    /** Signature algorithm mandated by the contract. */
    public static final String ALGORITHM = "SHA256withRSA";

    private LicenseSigner() {
    }

    /**
     * Signs the given license data with the provided private key.
     *
     * @param data       the license data to sign
     * @param privateKey the RSA private key (signing key pair)
     * @return the Base64-encoded signature
     */
    public static String sign(LicenseData data, PrivateKey privateKey) {
        try {
            String canonical = CanonicalStringBuilder.build(data);
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign license data", e);
        }
    }
}