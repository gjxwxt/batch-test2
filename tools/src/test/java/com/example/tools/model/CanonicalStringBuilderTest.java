package com.example.tools.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalStringBuilderTest {

    @Test
    void buildsCanonicalStringWithSaltPrefixAndFrozenFieldOrder() {
        LicenseData data = new LicenseData(
                "InforSuite", "9.0", "Acme Corp", "formal", "true", "2027-12-31",
                "customer-1", "InforSuiteAS", "SN-2026-0001",
                "true", "10", "8", "16384");

        String canonical = CanonicalStringBuilder.build(data);

        assertTrue(canonical.startsWith(LicenseData.SALT_PREFIX),
                "canonical string must start with the salt prefix");
        // Fields must appear in the frozen canonical order.
        int componentIdx = canonical.indexOf("component=InforSuite");
        int versionIdx = canonical.indexOf("version=9.0");
        int licenseeIdx = canonical.indexOf("licensee=Acme Corp");
        int serialIdx = canonical.indexOf("serial=SN-2026-0001");
        int maxMemoryIdx = canonical.indexOf("max-memory=16384");
        assertTrue(componentIdx >= 0);
        assertTrue(versionIdx > componentIdx);
        assertTrue(licenseeIdx > versionIdx);
        assertTrue(serialIdx > licenseeIdx);
        assertTrue(maxMemoryIdx > serialIdx);
    }

    @Test
    void omitsAbsentFieldsButKeepsOrderDeterministic() {
        LicenseData data = new LicenseData(
                "InforSuite", "9.0", "Acme Corp", "formal", "true", "2027-12-31",
                "customer-1", "InforSuiteAS", "SN-2026-0001",
                null, null, null, null);

        String canonical = CanonicalStringBuilder.build(data);

        // Absent optional fields must not appear.
        assertTrue(!canonical.contains("center-required"));
        assertTrue(!canonical.contains("max-instances"));
        assertTrue(!canonical.contains("max-cpus"));
        assertTrue(!canonical.contains("max-memory"));
        // Required fields still present in order.
        assertTrue(canonical.contains("component=InforSuite"));
        assertTrue(canonical.contains("serial=SN-2026-0001"));
    }

    @Test
    void sameDataProducesIdenticalCanonicalString() {
        LicenseData data = new LicenseData(
                "InforSuite", "9.0", "Acme Corp", "formal", "true", "2027-12-31",
                "customer-1", "InforSuiteAS", "SN-2026-0001",
                "true", "10", "8", "16384");
        assertEquals(CanonicalStringBuilder.build(data), CanonicalStringBuilder.build(data));
    }
}