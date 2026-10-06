package com.example.sdk.http;

import com.example.sdk.exception.SdkErrorCode;
import com.example.sdk.exception.SdkException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal HTTP transport for the SDK built on {@link java.net.http.HttpClient}.
 *
 * <p>Kept dependency-free (JDK only) so the SDK remains an embeddable JAR that
 * does not force a specific HTTP client or web framework on consumers.</p>
 */
public final class HttpTransport {

    private final HttpClient client;
    private final Duration timeout;

    public HttpTransport(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    /**
     * Perform a JSON POST request.
     *
     * @param url         full request URL
     * @param jsonBody    serialized JSON body
     * @param bearerToken optional bearer token (may be null)
     * @return the response body string
     * @throws SdkException on transport or non-2xx errors
     */
    public String postJson(String url, String jsonBody, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return send(builder.build());
    }

    /**
     * Perform a JSON GET request.
     *
     * @param url         full request URL
     * @param bearerToken optional bearer token (may be null)
     * @return the response body string
     * @throws SdkException on transport or non-2xx errors
     */
    public String getJson(String url, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET();
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return send(builder.build());
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Server returned HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException e) {
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Network error calling " + request.uri() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Request interrupted: " + e.getMessage(), e);
        }
    }
}