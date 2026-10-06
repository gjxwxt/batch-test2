package com.example.sdk.model;

/**
 * Response from {@code GET /api/v1/license/public-key}.
 *
 * <p>Contains the base64-encoded DER public key used to verify license
 * signatures. Per the dual-key contract (O6) this is the <em>issuing</em>
 * public key (verifies license signatures), distinct from the communication
 * key pair used for register/heartbeat.</p>
 */
public record PublicKeyResponse(
        String algorithm,
        String publicKeyBase64,
        Integer keySize) {

    public boolean isRsa2048() {
        return "RSA".equalsIgnoreCase(algorithm) && keySize != null && keySize == 2048;
    }
}