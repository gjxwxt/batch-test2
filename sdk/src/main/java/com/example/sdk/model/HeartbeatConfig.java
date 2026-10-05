package com.example.sdk.model;

/**
 * Response from {@code GET /api/v1/license/heartbeat-config}.
 *
 * <p>Carries the center's heartbeat cadence so the embedded client can schedule
 * its heartbeat loop without hard-coding an interval.</p>
 */
public record HeartbeatConfig(
        Integer intervalSeconds,
        Integer timeoutMultiplier,
        Integer offlineThresholdSeconds) {

    /** Default heartbeat interval (seconds) when the center omits it. */
    public static final int DEFAULT_INTERVAL_SECONDS = 30;

    public int effectiveIntervalSeconds() {
        return intervalSeconds != null && intervalSeconds > 0
                ? intervalSeconds : DEFAULT_INTERVAL_SECONDS;
    }
}