package com.example.sdk.crypto;

import com.example.sdk.exception.SdkErrorCode;
import com.example.sdk.exception.SdkException;
import com.example.sdk.model.LicenseInfo;
import com.example.sdk.model.SignatureFields;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseSignatureVerifierTest {

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(SignatureFields.RSA_KEY_SIZE);
        return generator.generateKeyPair();
    }

    private static String sign(String payload, PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance(SignatureFields.SIGNATURE_ALGORITHM);
        signer.initSign(privateKey);
        signer.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private static Map<String, String> sampleFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", "ias-server");
        fields.put("version", "1.0.0");
        fields.put("licensee", "Acme Corp");
        fields.put("license_mode", "formal");
        fields.put("formal", "true");
        fields.put("expiration", "2027-12-31");
        fields.put("userinfor", "production");
        fields.put("proname", "InforSuite");
        fields.put("serial", "LIC-2026-0001");
        fields.put("center-required", "true");
        fields.put("max-instances", "10");
        fields.put("max-cpus", "8");
        fields.put("max-memory", "16384");
        return fields;
    }

    @Test
    void canonicalPayloadUsesFrozenFieldOrderAndSaltPrefix() {
        String payload = LicenseSignatureVerifier.buildCanonicalPayload(sampleFields());

        String expected = "InforSuiteAuth2026_component=ias-server\n"
                + "InforSuiteAuth2026_version=1.0.0\n"
                + "InforSuiteAuth2026_licensee=Acme Corp\n"
                + "InforSuiteAuth2026_mode=formal\n"
                + "InforSuiteAuth2026_formal=true\n"
                + "InforSuiteAuth2026_expiration=2027-12-31\n"
                + "InforSuiteAuth2026_userinfor=production\n"
                + "InforSuiteAuth2026_proname=InforSuite\n"
                + "InforSuiteAuth2026_serial=LIC-2026-0001\n"
                + "InforSuiteAuth2026_center-required=true\n"
                + "InforSuiteAuth2026_max-instances=10\n"
                + "InforSuiteAuth2026_max-cpus=8\n"
                + "InforSuiteAuth2026_max-memory=16384\n";

        assertEquals(expected, payload);
    }

    @Test
    void missingFieldsAreEmittedAsEmptyValues() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", "x");
        String payload = LicenseSignatureVerifier.buildCanonicalPayload(fields);

        assertTrue(payload.startsWith("InforSuiteAuth2026_component=x\n"));
        assertTrue(payload.contains("InforSuiteAuth2026_version=\n"));
        assertTrue(payload.contains("InforSuiteAuth2026_max-memory=\n"));
    }

    @Test
    void validSignatureVerifiesTrue() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = sampleFields();
        String payload = LicenseSignatureVerifier.buildCanonicalPayload(fields);
        String signature = sign(payload, keyPair.getPrivate());

        LicenseInfo license = LicenseInfo.fromMap(fields);
        LicenseInfo signed = new LicenseInfo(
                license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(),
                license.licenseMode(), license.formal(), license.expiration(),
                license.userinfor(), license.centerRequired(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.status(), signature);

        assertTrue(LicenseSignatureVerifier.verify(signed, encodePublicKey(keyPair.getPublic())));
    }

    @Test
    void tamperedFieldFailsVerification() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, String> fields = sampleFields();
        String payload = LicenseSignatureVerifier.buildCanonicalPayload(fields);
        String signature = sign(payload, keyPair.getPrivate());

        // Tamper with a signed field.
        fields.put("max-instances", "999");
        LicenseInfo license = LicenseInfo.fromMap(fields);
        LicenseInfo signed = new LicenseInfo(
                license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(),
                license.licenseMode(), license.formal(), license.expiration(),
                license.userinfor(), license.centerRequired(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.status(), signature);

        assertFalse(LicenseSignatureVerifier.verify(signed, encodePublicKey(keyPair.getPublic())));
    }

    @Test
    void signatureFromDifferentKeyFailsVerification() throws Exception {
        KeyPair issuer = generateKeyPair();
        KeyPair attacker = generateKeyPair();
        Map<String, String> fields = sampleFields();
        String payload = LicenseSignatureVerifier.buildCanonicalPayload(fields);
        String signature = sign(payload, attacker.getPrivate());

        LicenseInfo license = LicenseInfo.fromMap(fields);
        LicenseInfo signed = new LicenseInfo(
                license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(),
                license.licenseMode(), license.formal(), license.expiration(),
                license.userinfor(), license.centerRequired(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.status(), signature);

        assertFalse(LicenseSignatureVerifier.verify(signed, encodePublicKey(issuer.getPublic())));
    }

    @Test
    void missingSignatureThrowsLicenseError() throws Exception {
        KeyPair keyPair = generateKeyPair();
        LicenseInfo license = LicenseInfo.fromMap(sampleFields());

        SdkException ex = assertThrows(SdkException.class,
                () -> LicenseSignatureVerifier.verify(license, encodePublicKey(keyPair.getPublic())));
        assertEquals(SdkErrorCode.LICENSE_003.code(), ex.getCode());
    }

    @Test
    void invalidBase64PublicKeyThrowsLicenseError() {
        LicenseInfo license = LicenseInfo.fromMap(sampleFields());
        LicenseInfo signed = new LicenseInfo(
                license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(),
                license.licenseMode(), license.formal(), license.expiration(),
                license.userinfor(), license.centerRequired(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.status(), "AAAA");

        SdkException ex = assertThrows(SdkException.class,
                () -> LicenseSignatureVerifier.verify(signed, "not-base64!!!"));
        assertEquals(SdkErrorCode.LICENSE_003.code(), ex.getCode());
    }
}