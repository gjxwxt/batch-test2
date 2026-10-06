package com.example.sdk.model;

/**
 * Response from {@code POST /api/v1/license/register}.
 *
 * <p>On success the center returns the assigned {@code instanceId} and the
 * heartbeat interval. Field names mirror the server's {@code RegisterResponse}
 * DTO ({@code instanceId}/{@code status}/{@code heartbeatInterval}/{@code message}).</p>
 */
public record RegisterResponse(
        String instanceId,
        String status,
        Integer heartbeatInterval,
        String message) {

    public boolean isSuccess() {
        return "ONLINE".equalsIgnoreCase(status);
    }
}