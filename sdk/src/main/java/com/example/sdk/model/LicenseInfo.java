package com.example.sdk.model;

import java.util.Map;

/**
 * Immutable representation of a parsed license issued by the IAS Auth Center.
 *
 * <p>Fields mirror the shared {@code license} table contract. The {@code signature}
 * is the base64-encoded RSA-2048 + SHA256withRSA signature over the canonical
 * payload built from {@link SignatureFields#CANONICAL_ORDER}.</p>
 */
public record LicenseInfo(
        String serial,
        String licenseName,
        String proname,
        String component,
        String version,
        String licensee,
        String licenseMode,
        String formal,
        String expiration,
        String userinfor,
        String centerRequired,
        Integer maxInstances,
        Integer maxCpus,
        Integer maxMemory,
        String status,
        String signature) {

    /**
     * Build a {@link LicenseInfo} from a flat map of raw field names (as they
     * appear in the license file / API payload). Unknown keys are ignored.
     *
     * @param raw map of raw field name to string value
     * @return a populated {@link LicenseInfo}
     */
    public static LicenseInfo fromMap(Map<String, String> raw) {
        return new LicenseInfo(
                raw.get("serial"),
                raw.get("license_name"),
                raw.get("proname"),
                raw.get("component"),
                raw.get("version"),
                raw.get("licensee"),
                raw.get("license_mode"),
                raw.get("formal"),
                raw.get("expiration"),
                raw.get("userinfor"),
                raw.get("center-required"),
                toInt(raw.get("max-instances")),
                toInt(raw.get("max-cpus")),
                toInt(raw.get("max-memory")),
                raw.get("status"),
                raw.get("signature"));
    }

    private static Integer toInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}