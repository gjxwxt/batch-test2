package com.example.tools.model;

import java.util.Map;

/**
 * Builds the deterministic canonical string that is signed with
 * SHA256withRSA. The canonical string is derived from the frozen field order
 * and the shared salt prefix so that the server-side verifier can reproduce
 * the exact same bytes.
 */
public final class CanonicalStringBuilder {

    private CanonicalStringBuilder() {
    }

    /**
     * Builds the canonical signing string for the given license data.
     *
     * <p>Format: {@code InforSuiteAuth2026_<field1>=<value1>&<field2>=<value2>&...}
     * Fields are emitted in the frozen canonical order; absent fields are
     * skipped. The salt prefix is prepended so that signatures are bound to
     * the IAS Auth Center domain.
     */
    public static String build(LicenseData data) {
        StringBuilder sb = new StringBuilder(LicenseData.SALT_PREFIX);
        Map<String, String> map = data.toCanonicalMap();
        boolean first = true;
        for (String field : LicenseData.CANONICAL_FIELD_ORDER) {
            String value = map.get(field);
            if (value == null) {
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
}