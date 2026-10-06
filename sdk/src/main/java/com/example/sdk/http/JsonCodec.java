package com.example.sdk.http;

import com.example.sdk.exception.SdkErrorCode;
import com.example.sdk.exception.SdkException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin JSON (de)serialization wrapper around Jackson used by the SDK transport.
 *
 * <p>Kept internal so callers never depend on Jackson types directly.</p>
 */
public final class JsonCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonCodec() {
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Failed to serialize request: " + e.getMessage(), e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new SdkException(SdkErrorCode.SYS_001.code(),
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }
}