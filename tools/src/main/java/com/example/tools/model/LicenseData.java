package com.example.tools.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable representation of the license fields that participate in the
 * canonical signature. The field set and ordering are frozen by the shared
 * contract (contract/signature/signature-canonical.md) and must not be
 * reordered — the signature is computed over the canonical string built from
 * these fields in exactly this order.
 *
 * <p>Canonical field order (frozen): component, version, licensee, mode,
 * formal, expiration, userinfor, proname, serial, center-required,
 * max-instances, max-cpus, max-memory.
 */
public record LicenseData(
        String component,
        String version,
        String licensee,
        String mode,
        String formal,
        String expiration,
        String userinfor,
        String proname,
        String serial,
        String centerRequired,
        String maxInstances,
        String maxCpus,
        String maxMemory) {

    /** The frozen canonical field order used to build the signing string. */
    public static final String[] CANONICAL_FIELD_ORDER = {
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

    /** Salt prefix mandated by the shared contract. */
    public static final String SALT_PREFIX = "InforSuiteAuth2026_";

    /**
     * Returns the license fields as an ordered map keyed by the canonical
     * field names. Missing (null) fields are omitted so that the canonical
     * string stays deterministic.
     */
    public Map<String, String> toCanonicalMap() {
        Map<String, String> map = new LinkedHashMap<>();
        putIfPresent(map, "component", component);
        putIfPresent(map, "version", version);
        putIfPresent(map, "licensee", licensee);
        putIfPresent(map, "mode", mode);
        putIfPresent(map, "formal", formal);
        putIfPresent(map, "expiration", expiration);
        putIfPresent(map, "userinfor", userinfor);
        putIfPresent(map, "proname", proname);
        putIfPresent(map, "serial", serial);
        putIfPresent(map, "center-required", centerRequired);
        putIfPresent(map, "max-instances", maxInstances);
        putIfPresent(map, "max-cpus", maxCpus);
        putIfPresent(map, "max-memory", maxMemory);
        return map;
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }
}