package com.example.tools.crypto;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KeyPairGeneratorTest {

    @Test
    void generatesRsa2048KeyPair() {
        KeyPair pair = KeyPairGenerator.generate();
        assertNotNull(pair.getPrivate());
        assertNotNull(pair.getPublic());
        assertEquals(2048, ((RSAPublicKey) pair.getPublic()).getModulus().bitLength());
        assertEquals(2048, ((RSAPrivateKey) pair.getPrivate()).getModulus().bitLength());
    }

    @Test
    void dualKeysAreDistinct() {
        // O6: signing and communication key pairs must never be reused.
        KeyPair signing = KeyPairGenerator.generate();
        KeyPair communication = KeyPairGenerator.generate();
        assertNotEquals(signing.getPrivate().getEncoded(), communication.getPrivate().getEncoded());
        assertNotEquals(signing.getPublic().getEncoded(), communication.getPublic().getEncoded());
    }
}