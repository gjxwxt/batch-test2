package com.example.tools.cli;

import com.example.tools.crypto.KeyPairGenerator;
import com.example.tools.crypto.LicenseSigner;
import com.example.tools.crypto.LicenseVerifier;
import com.example.tools.model.LicenseData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Command-line entry point for the IAS Auth Center license generation tool.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code genkey <outDir>} — generate a dual RSA-2048 key pair set
 *       (signing + communication) and write PEM files to {@code outDir}.</li>
 *   <li>{@code sign <privateKeyPem> <component> <version> <licensee> <mode>
 *       <formal> <expiration> <userinfor> <proname> <serial>
 *       [center-required] [max-instances] [max-cpus] [max-memory]} — sign a
 *       license and print the Base64 signature.</li>
 *   <li>{@code verify <publicKeyPem> <signature> <component> <version>
 *       <licensee> <mode> <formal> <expiration> <userinfor> <proname>
 *       <serial> [center-required] [max-instances] [max-cpus] [max-memory]}
 *       — verify a license signature and exit 0 on success, 1 on failure.</li>
 * </ul>
 */
public final class LicenseToolCli {

    private static final String USAGE = """
            Usage:
              tools genkey <outDir>
              tools sign <privateKeyPem> <component> <version> <licensee> <mode> <formal> <expiration> <userinfor> <proname> <serial> [center-required] [max-instances] [max-cpus] [max-memory]
              tools verify <publicKeyPem> <signature> <component> <version> <licensee> <mode> <formal> <expiration> <userinfor> <proname> <serial> [center-required] [max-instances] [max-cpus] [max-memory]
            """;

    private LicenseToolCli() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println(USAGE);
            System.exit(2);
        }
        try {
            switch (args[0]) {
                case "genkey" -> runGenKey(args);
                case "sign" -> runSign(args);
                case "verify" -> runVerify(args);
                default -> {
                    System.err.println("Unknown command: " + args[0]);
                    System.err.println(USAGE);
                    System.exit(2);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runGenKey(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("genkey requires an output directory");
            System.exit(2);
        }
        Path outDir = Path.of(args[1]);
        Files.createDirectories(outDir);

        // Dual-key scheme (O6): signing and communication key pairs are distinct.
        KeyPair signing = KeyPairGenerator.generate();
        KeyPair communication = KeyPairGenerator.generate();

        writePem(outDir.resolve("signing-private.pem"), signing.getPrivate().getEncoded(), "PRIVATE KEY");
        writePem(outDir.resolve("signing-public.pem"), signing.getPublic().getEncoded(), "PUBLIC KEY");
        writePem(outDir.resolve("communication-private.pem"), communication.getPrivate().getEncoded(), "PRIVATE KEY");
        writePem(outDir.resolve("communication-public.pem"), communication.getPublic().getEncoded(), "PUBLIC KEY");

        System.out.println("Generated dual RSA-2048 key pairs in " + outDir.toAbsolutePath());
        System.out.println("  signing-private.pem / signing-public.pem   (sign licenses)");
        System.out.println("  communication-private.pem / communication-public.pem   (register/heartbeat)");
    }

    private static void runSign(String[] args) throws Exception {
        if (args.length < 11) {
            System.err.println("sign requires privateKeyPem and 9 license fields");
            System.err.println(USAGE);
            System.exit(2);
        }
        PrivateKey privateKey = readPrivateKey(Path.of(args[1]));
        LicenseData data = parseLicense(args, 2);
        String signature = LicenseSigner.sign(data, privateKey);
        System.out.println(signature);
    }

    private static void runVerify(String[] args) throws Exception {
        if (args.length < 12) {
            System.err.println("verify requires publicKeyPem, signature and 9 license fields");
            System.err.println(USAGE);
            System.exit(2);
        }
        PublicKey publicKey = readPublicKey(Path.of(args[1]));
        String signature = args[2];
        LicenseData data = parseLicense(args, 3);
        boolean valid = LicenseVerifier.verify(data, publicKey, signature);
        if (valid) {
            System.out.println("SIGNATURE_VALID");
            System.exit(0);
        } else {
            System.out.println("SIGNATURE_INVALID");
            System.exit(1);
        }
    }

    private static LicenseData parseLicense(String[] args, int start) {
        String component = args[start];
        String version = args[start + 1];
        String licensee = args[start + 2];
        String mode = args[start + 3];
        String formal = args[start + 4];
        String expiration = args[start + 5];
        String userinfor = args[start + 6];
        String proname = args[start + 7];
        String serial = args[start + 8];
        String centerRequired = argOrNull(args, start + 9);
        String maxInstances = argOrNull(args, start + 10);
        String maxCpus = argOrNull(args, start + 11);
        String maxMemory = argOrNull(args, start + 12);
        return new LicenseData(component, version, licensee, mode, formal, expiration,
                userinfor, proname, serial, centerRequired, maxInstances, maxCpus, maxMemory);
    }

    private static String argOrNull(String[] args, int index) {
        return index < args.length ? args[index] : null;
    }

    private static PrivateKey readPrivateKey(Path path) throws Exception {
        byte[] der = readPem(path, "PRIVATE KEY");
        var factory = java.security.KeyFactory.getInstance("RSA");
        return factory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(der));
    }

    private static PublicKey readPublicKey(Path path) throws Exception {
        byte[] der = readPem(path, "PUBLIC KEY");
        var factory = java.security.KeyFactory.getInstance("RSA");
        return factory.generatePublic(new java.security.spec.X509EncodedKeySpec(der));
    }

    private static byte[] readPem(Path path, String type) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int startIdx = content.indexOf(begin);
        int endIdx = content.indexOf(end);
        if (startIdx < 0 || endIdx < 0) {
            throw new IOException("Invalid PEM file (missing " + type + " markers): " + path);
        }
        String base64 = content.substring(startIdx + begin.length(), endIdx)
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static void writePem(Path path, byte[] der, String type) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(der);
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        sb.append("-----END ").append(type).append("-----\n");
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }
}