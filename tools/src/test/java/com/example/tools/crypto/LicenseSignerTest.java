package com.example.tools.crypto;

import com.example.tools.model.LicenseData;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseSignerTest {

    private static LicenseData sampleData() {
        return new LicenseData(
                "InforSuite", "9.0", "Acme Corp", "formal", "true", "2027-12-31",
                "customer-1", "InforSuiteAS", "SN-2026-0001",
                "true", "10", "8", "16384");
    }

    @Test
    void signProducesNonEmptyBase64Signature() {
        KeyPair pair = KeyPairGenerator.generate();
        String signature = LicenseSigner.sign(sampleData(), pair.getPrivate());
        assertNotNull(signature);
        assertTrue(signature.length() > 0);
    }

    @Test
    void verifyRoundTripSucceeds() {
        KeyPair pair = KeyPairGenerator.generate();
        LicenseData data = sampleData();
        String signature = LicenseSigner.sign(data, pair.getPrivate());
        assertTrue(LicenseVerifier.verify(data, pair.getPublic(), signature));
    }

    @Test
    void verifyFailsWhenLicenseFieldIsTampered() {
        KeyPair pair = KeyPairGenerator.generate();
        LicenseData original = sampleData();
        String signature = LicenseSigner.sign(original, pair.getPrivate());

        LicenseData tampered = new LicenseData(
                "InforSuite", "9.0", "Evil Corp", "formal", "true", "2027-12-31",
                "customer-1", "InforSuiteAS", "SN-2026-0001",
                "true", "10", "8", "16384");

        assertFalse(LicenseVerifier.verify(tampered, pair.getPublic(), signature));
    }

    @Test
    void verifyFailsWithWrongPublicKey() {
        KeyPair signer = KeyPairGenerator.generate();
        KeyPair other = KeyPairGenerator.generate();
        LicenseData data = sampleData();
        String signature = LicenseSigner.sign(data, signer.getPrivate());
        assertFalse(LicenseVerifier.verify(data, other.getPublic(), signature));
    }

    @Test
    void verifyFailsOnGarbageSignature() {
        KeyPair pair = KeyPairGenerator.generate();
        assertFalse(LicenseVerifier.verify(sampleData(), pair.getPublic(), "not-a-valid-signature"));
    }
}