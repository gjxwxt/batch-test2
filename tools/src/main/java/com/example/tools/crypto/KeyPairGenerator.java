package com.example.tools.crypto;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

/**
 * Generates RSA-2048 key pairs for the IAS Auth Center.
 *
 * <p>The shared contract mandates a dual-key scheme (O6): the signing key pair
 * used to sign licenses and the communication key pair used for
 * register/heartbeat must never be reused. Callers must therefore obtain two
 * distinct key pairs via {@link #generate()} and use them for their dedicated
 * purposes.
 */
public final class KeyPairGenerator {

    /** RSA key size mandated by the contract. */
    public static final int KEY_SIZE = 2048;

    private KeyPairGenerator() {
    }

    /**
     * Generates a fresh RSA-2048 key pair.
     *
     * @return a newly generated RSA-2048 {@link KeyPair}
     */
    public static KeyPair generate() {
        try {
            java.security.KeyPairGenerator generator =
                    java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            // RSA is guaranteed by the JCA; this should never happen.
            throw new IllegalStateException("RSA algorithm unavailable", e);
        }
    }
}