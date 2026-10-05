package com.example.sdk.model;

/**
 * Response from {@code POST /api/v1/license/register}.
 *
 * <p>On success the center returns the assigned {@code instanceId} and the
 * {@code licenseId} bound to the registered instance.</p>
 */
public record RegisterResponse(
        String code,
        String message,
        String instanceId,
        String licenseId,
        String clientUuid) {

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(code);
    }
}