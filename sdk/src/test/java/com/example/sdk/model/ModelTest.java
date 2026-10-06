package com.example.sdk.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {

    @Test
    void licenseInfoFromMapMapsRawFields() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("serial", "LIC-1");
        raw.put("license_name", "Enterprise");
        raw.put("component", "ias-server");
        raw.put("max-instances", "10");
        raw.put("max-cpus", "8");
        raw.put("max-memory", "16384");
        raw.put("signature", "abc");

        LicenseInfo license = LicenseInfo.fromMap(raw);

        assertEquals("LIC-1", license.serial());
        assertEquals("Enterprise", license.licenseName());
        assertEquals("ias-server", license.component());
        assertEquals(Integer.valueOf(10), license.maxInstances());
        assertEquals(Integer.valueOf(8), license.maxCpus());
        assertEquals(Integer.valueOf(16384), license.maxMemory());
        assertEquals("abc", license.signature());
    }

    @Test
    void licenseInfoFromMapHandlesMissingAndInvalidNumbers() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("max-instances", "not-a-number");

        LicenseInfo license = LicenseInfo.fromMap(raw);

        assertNull(license.serial());
        assertNull(license.maxInstances());
    }

    @Test
    void registerResponseSuccessDetection() {
        assertTrue(new RegisterResponse("SUCCESS", "ok", "i1", "l1", "u1").isSuccess());
        assertFalse(new RegisterResponse("LICENSE_001", "denied", null, null, null).isSuccess());
    }

    @Test
    void heartbeatResponseSuccessDetection() {
        assertTrue(new HeartbeatResponse("SUCCESS", "ok", true, 30L).isSuccess());
        assertFalse(new HeartbeatResponse("WARNING", "quota", false, 5L).isSuccess());
    }

    @Test
    void heartbeatConfigEffectiveIntervalFallsBackToDefault() {
        assertEquals(30, new HeartbeatConfig(null, null, null).effectiveIntervalSeconds());
        assertEquals(45, new HeartbeatConfig(45, 3, 135).effectiveIntervalSeconds());
        assertEquals(30, new HeartbeatConfig(0, 3, 135).effectiveIntervalSeconds());
    }

    @Test
    void publicKeyResponseDetectsRsa2048() {
        assertTrue(new PublicKeyResponse("RSA", "base64", 2048).isRsa2048());
        assertFalse(new PublicKeyResponse("RSA", "base64", 1024).isRsa2048());
        assertFalse(new PublicKeyResponse("EC", "base64", 2048).isRsa2048());
    }
}