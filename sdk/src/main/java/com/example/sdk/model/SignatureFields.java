package com.example.sdk.model;

/**
 * Canonical field order used to build the license signature payload.
 *
 * <p>The shared contract (wp-0) freezes the exact ordering of fields that are
 * concatenated (with the salt prefix) and signed with RSA-2048 + SHA256withRSA.
 * Any reordering breaks signature verification, so this list must not change
 * without a new approved contract version.</p>
 */
public final class SignatureFields {

    private SignatureFields() {
    }

    /** Canonical field order for license signature payloads. */
    public static final String[] CANONICAL_ORDER = {
            "component",
            "version",
            "licensee",
            "mode",
            "formal",
            "expiration",
            "userinfor",
            "proname",
            "serial",
            "center-required",
            "max-instances",
            "max-cpus",
            "max-memory"
    };

    /** Salt prefix prepended to the canonical payload before signing. */
    public static final String SALT_PREFIX = "InforSuiteAuth2026_";

    /** Signature algorithm: RSA-2048 with SHA-256. */
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /** RSA key size in bits. */
    public static final int RSA_KEY_SIZE = 2048;
}