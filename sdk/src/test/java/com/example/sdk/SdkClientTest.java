package com.example.sdk;

import com.example.sdk.exception.SdkErrorCode;
import com.example.sdk.exception.SdkException;
import com.example.sdk.model.HeartbeatConfig;
import com.example.sdk.model.HeartbeatRequest;
import com.example.sdk.model.HeartbeatResponse;
import com.example.sdk.model.PublicKeyResponse;
import com.example.sdk.model.RegisterRequest;
import com.example.sdk.model.RegisterResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdkClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void stub(String path, String method, String responseBody) {
        server.createContext(path, exchange -> {
            if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    void fetchPublicKeyParsesResponse() {
        stub("/api/v1/license/public-key", "GET",
                "{\"algorithm\":\"RSA\",\"keySize\":2048,\"publicKey\":\"QUJD\"}");

        SdkClient client = SdkClient.create(baseUrl, Duration.ofSeconds(5));
        PublicKeyResponse response = client.fetchPublicKey();

        assertEquals("RSA", response.algorithm());
        assertEquals("QUJD", response.publicKey());
        assertTrue(response.isRsa2048());
    }

    @Test
    void fetchHeartbeatConfigParsesResponse() {
        stub("/api/v1/license/heartbeat-config", "GET",
                "{\"heartbeatInterval\":45,\"timeoutCount\":3,\"timeoutSeconds\":135}");

        SdkClient client = SdkClient.create(baseUrl, Duration.ofSeconds(5));
        HeartbeatConfig config = client.fetchHeartbeatConfig();

        assertEquals(45, config.effectiveIntervalSeconds());
    }

    @Test
    void registerPostsJsonAndParsesResponse() {
        stub("/api/v1/license/register", "POST",
                "{\"instanceId\":\"inst-1\",\"status\":\"ONLINE\",\"heartbeatInterval\":30,\"message\":\"注册成功\"}");

        SdkClient client = SdkClient.create(baseUrl, Duration.ofSeconds(5));
        RegisterRequest request = RegisterRequest.builder()
                .serial("serial-001")
                .clientUuid("uuid-1")
                .proname("InforSuite")
                .hostname("host-a")
                .build();

        RegisterResponse response = client.register(request);

        assertTrue(response.isSuccess());
        assertEquals("inst-1", response.instanceId());
        assertTrue(lastBody.get().contains("\"clientUuid\":\"uuid-1\""));
    }

    @Test
    void heartbeatPostsJsonAndParsesResponse() {
        stub("/api/v1/license/heartbeat", "POST",
                "{\"instanceId\":\"inst-1\",\"status\":\"ONLINE\",\"serverTime\":\"2026-10-05T00:00:00Z\",\"message\":\"心跳成功\"}");

        SdkClient client = SdkClient.create(baseUrl, Duration.ofSeconds(5));
        HeartbeatRequest request = HeartbeatRequest.builder()
                .instanceId("inst-1")
                .serial("serial-001")
                .currentCpus(4)
                .build();

        HeartbeatResponse response = client.heartbeat(request);

        assertTrue(response.isSuccess());
        assertEquals("inst-1", response.instanceId());
        assertTrue(lastBody.get().contains("\"instanceId\":\"inst-1\""));
    }

    @Test
    void applyLicenseFilePostsPayload() {
        stub("/api/v1/license/file-apply", "POST", "{\"file\":\"LICENSE.bxb\"}");

        SdkClient client = SdkClient.create(baseUrl, Duration.ofSeconds(5));
        String result = client.applyLicenseFile("{\"serial\":\"LIC-1\"}");

        assertEquals("{\"file\":\"LICENSE.bxb\"}", result);
        assertEquals("{\"serial\":\"LIC-1\"}", lastBody.get());
    }

    @Test
    void blankBaseUrlRejected() {
        SdkException ex = assertThrows(SdkException.class, () -> SdkClient.create("  "));
        assertEquals(SdkErrorCode.PARAM_001.code(), ex.getCode());
    }

    @Test
    void serverErrorThrowsSdkException() {
        server.createContext("/api/v1/license/heartbeat", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        SdkClient client = SdkClient.create(baseUrl, Duration.ofSeconds(5));
        HeartbeatRequest request = HeartbeatRequest.builder().instanceId("i1").build();

        SdkException ex = assertThrows(SdkException.class, () -> client.heartbeat(request));
        assertEquals(SdkErrorCode.SYS_001.code(), ex.getCode());
    }
}