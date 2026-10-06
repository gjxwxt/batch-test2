package com.example.sdk;

import com.example.sdk.exception.SdkErrorCode;
import com.example.sdk.exception.SdkException;
import com.example.sdk.http.HttpTransport;
import com.example.sdk.http.JsonCodec;
import com.example.sdk.model.HeartbeatConfig;
import com.example.sdk.model.HeartbeatRequest;
import com.example.sdk.model.HeartbeatResponse;
import com.example.sdk.model.PublicKeyResponse;
import com.example.sdk.model.RegisterRequest;
import com.example.sdk.model.RegisterResponse;

import java.time.Duration;

/**
 * Embeddable client for the IAS Auth Center license service.
 *
 * <p>This is the primary entry point for applications that embed the SDK. It
 * talks to the center's client-facing (no-auth) endpoints:</p>
 * <ul>
 *   <li>{@code GET  /api/v1/license/public-key}</li>
 *   <li>{@code GET  /api/v1/license/heartbeat-config}</li>
 *   <li>{@code POST /api/v1/license/register}</li>
 *   <li>{@code POST /api/v1/license/heartbeat}</li>
 *   <li>{@code POST /api/v1/license/file-apply}</li>
 * </ul>
 *
 * <p>Thread-safe: the underlying {@link HttpTransport} is immutable and safe to
 * share across threads.</p>
 */
public final class SdkClient {

    private final String baseUrl;
    private final HttpTransport transport;

    private SdkClient(String baseUrl, HttpTransport transport) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.transport = transport;
    }

    /**
     * Create a client with default timeouts.
     *
     * @param baseUrl center base URL, e.g. {@code http://localhost:8080}
     * @return a configured {@link SdkClient}
     */
    public static SdkClient create(String baseUrl) {
        return create(baseUrl, Duration.ofSeconds(10));
    }

    /**
     * Create a client with a custom request timeout.
     *
     * @param baseUrl center base URL
     * @param timeout per-request timeout
     * @return a configured {@link SdkClient}
     */
    public static SdkClient create(String baseUrl, Duration timeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new SdkException(SdkErrorCode.PARAM_001.code(), "baseUrl must not be blank");
        }
        return new SdkClient(baseUrl, new HttpTransport(timeout));
    }

    /**
     * Fetch the issuing public key used to verify license signatures.
     *
     * @return the {@link PublicKeyResponse}
     */
    public PublicKeyResponse fetchPublicKey() {
        String json = transport.getJson(baseUrl + "/api/v1/license/public-key", null);
        return JsonCodec.read(json, PublicKeyResponse.class);
    }

    /**
     * Fetch the heartbeat cadence configured by the center.
     *
     * @return the {@link HeartbeatConfig}
     */
    public HeartbeatConfig fetchHeartbeatConfig() {
        String json = transport.getJson(baseUrl + "/api/v1/license/heartbeat-config", null);
        return JsonCodec.read(json, HeartbeatConfig.class);
    }

    /**
     * Register a client instance with the center.
     *
     * @param request the registration payload
     * @return the {@link RegisterResponse}
     */
    public RegisterResponse register(RegisterRequest request) {
        if (request == null) {
            throw new SdkException(SdkErrorCode.PARAM_001.code(), "register request must not be null");
        }
        String json = transport.postJson(baseUrl + "/api/v1/license/register",
                JsonCodec.write(request), null);
        return JsonCodec.read(json, RegisterResponse.class);
    }

    /**
     * Send a heartbeat for a registered instance.
     *
     * @param request the heartbeat payload
     * @return the {@link HeartbeatResponse}
     */
    public HeartbeatResponse heartbeat(HeartbeatRequest request) {
        if (request == null) {
            throw new SdkException(SdkErrorCode.PARAM_001.code(), "heartbeat request must not be null");
        }
        String json = transport.postJson(baseUrl + "/api/v1/license/heartbeat",
                JsonCodec.write(request), null);
        return JsonCodec.read(json, HeartbeatResponse.class);
    }

    /**
     * Apply for a license file from the center.
     *
     * @param payload serialized apply payload (product/instance context)
     * @return the raw response body (typically the license file content)
     */
    public String applyLicenseFile(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new SdkException(SdkErrorCode.PARAM_001.code(), "apply payload must not be blank");
        }
        return transport.postJson(baseUrl + "/api/v1/license/file-apply", payload, null);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}