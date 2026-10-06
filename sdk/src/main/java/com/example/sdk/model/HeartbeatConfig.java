package com.example.sdk.model;

/**
 * Response from {@code GET /api/v1/license/heartbeat-config}.
 *
 * <p>Carries the center's heartbeat cadence so the embedded client can schedule
 * its heartbeat loop without hard-coding an interval.</p>
 *
 * <p>Field names mirror the server's {@code HeartbeatConfigResponse} DTO
 * ({@code heartbeatInterval}/{@code timeoutCount}/{@code timeoutSeconds}).</p>
 */
public record HeartbeatConfig(
        Integer heartbeatInterval,
        Integer timeoutCount,
        Integer timeoutSeconds) {

    /** Default heartbeat interval (seconds) when the center omits it. */
    public static final int DEFAULT_INTERVAL_SECONDS = 30;

    public int effectiveIntervalSeconds() {
        return heartbeatInterval != null && heartbeatInterval > 0
                ? heartbeatInterval : DEFAULT_INTERVAL_SECONDS;
    }
}