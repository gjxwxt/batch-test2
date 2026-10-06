package com.example.sdk.crypto;

import com.example.sdk.exception.SdkErrorCode;
import com.example.sdk.exception.SdkException;
import com.example.sdk.model.HeartbeatRequest;
import com.example.sdk.model.RegisterRequest;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Signs client-to-center communication requests (register / heartbeat /
 * file-apply) with the client's communication key pair (dual-key contract, O6).
 *
 * <p>The canonical payload and signature algorithm must match the server's
 * {@code CommunicationSignature} so the center can verify the request.</p>
 */
public final class CommunicationSigner {

    /** Signature algorithm mandated by the contract. */
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private CommunicationSigner() {
    }

    /**
     * Build the canonical payload for a register request, matching the server's
     * {@code buildRegisterCanonical}: {@code serial + clientUuid + proname +
     * productType + productVersion + productSpec + hostname + ipAddress + mac +
     * machineType} (concatenated, nulls as empty).
     */
    public static String buildRegisterCanonical(RegisterRequest request) {
        return join(request.serial(), request.clientUuid(), request.proname(),
                request.productType(), request.productVersion(), request.productSpec(),
                request.hostname(), request.ipAddress(), request.mac(), request.machineType());
    }

    /**
     * Build the canonical payload for a heartbeat request, matching the server's
     * {@code buildHeartbeatCanonical}: {@code instanceId + serial}.
     */
    public static String buildHeartbeatCanonical(HeartbeatRequest request) {
        return join(request.instanceId(), request.serial());
    }

    /**
     * Sign a canonical payload with the client's communication private key.
     *
     * @param canonical  the canonical payload to sign
     * @param privateKey the client's communication private key
     * @return Base64-encoded signature
     */
    public static String sign(String canonical, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Failed to sign communication request: " + e.getMessage(), e);
        }
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part == null ? "" : part);
        }
        return sb.toString();
    }
}