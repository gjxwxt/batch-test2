package com.example.sdk.model;

/**
 * Response from {@code POST /api/v1/license/heartbeat}.
 *
 * <p>When the center detects a quota breach or a disabled license it returns a
 * non-{@code SUCCESS} code so the embedded client can react (e.g. throttle or
 * stop the product).</p>
 */
public record HeartbeatResponse(
        String code,
        String message,
        Boolean accepted,
        Long nextHeartbeatIntervalSeconds) {

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(code);
    }
}