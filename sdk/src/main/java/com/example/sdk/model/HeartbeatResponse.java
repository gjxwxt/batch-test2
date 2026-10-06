package com.example.sdk.model;

/**
 * Response from {@code POST /api/v1/license/heartbeat}.
 *
 * <p>Field names mirror the server's {@code HeartbeatResponse} DTO
 * ({@code instanceId}/{@code status}/{@code serverTime}/{@code message}).</p>
 */
public record HeartbeatResponse(
        String instanceId,
        String status,
        String serverTime,
        String message) {

    public boolean isSuccess() {
        return "ONLINE".equalsIgnoreCase(status);
    }
}